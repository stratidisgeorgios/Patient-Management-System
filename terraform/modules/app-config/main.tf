terraform {
  required_providers {
    aws  = { source = "hashicorp/aws", version = "~> 5.0" }
    helm = { source = "hashicorp/helm", version = "~> 2.0" }
  }
}

# ── AWS Secrets Manager ───────────────────────────────────────────────────────

resource "aws_secretsmanager_secret" "patient_system" {
  name                    = "patient-system/prod"
  recovery_window_in_days = 0

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "patient_system" {
  secret_id = aws_secretsmanager_secret.patient_system.id
  secret_string = jsonencode({
    rds-address            = var.rds_address
    db-password            = var.db_password
    msk-bootstrap-brokers  = var.msk_bootstrap_brokers
    opensearch-endpoint    = var.opensearch_endpoint
    cognito-user-pool-id   = var.cognito_user_pool_id
    s3-bucket-name         = var.s3_bucket_name
    import-queue-url       = var.import_queue_url
  })
}

# ── External Secrets Operator ─────────────────────────────────────────────────

resource "helm_release" "external_secrets" {
  name             = "external-secrets"
  repository       = "https://charts.external-secrets.io"
  chart            = "external-secrets"
  version          = "0.9.13"
  namespace        = "external-secrets"
  create_namespace = true

  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.eso.arn
  }
  set {
    name  = "serviceAccount.name"
    value = "external-secrets"
  }
}

# ── IRSA role for External Secrets Operator ───────────────────────────────────

locals {
  # Extract the OIDC issuer URL from the provider ARN (part after "oidc-provider/")
  oidc_issuer_url = element(split("oidc-provider/", var.oidc_provider_arn), 1)
}

data "aws_iam_policy_document" "eso_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_url}:sub"
      values   = ["system:serviceaccount:external-secrets:external-secrets"]
    }
  }
}

resource "aws_iam_role" "eso" {
  name               = "${var.environment}-external-secrets-operator"
  assume_role_policy = data.aws_iam_policy_document.eso_assume.json
  tags               = var.tags
}

resource "aws_iam_role_policy" "eso_secrets" {
  name = "read-patient-system-secrets"
  role = aws_iam_role.eso.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
      Resource = aws_secretsmanager_secret.patient_system.arn
    }]
  })
}

# ── IRSA: patient-service (S3 + SQS) ─────────────────────────────────────────

module "patient_service_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name = "${var.environment}-patient-service"

  oidc_providers = {
    main = {
      provider_arn               = var.oidc_provider_arn
      namespace_service_accounts = ["patient-system:patient-service"]
    }
  }

  tags = var.tags
}

resource "aws_iam_role_policy" "patient_service_s3" {
  name = "s3-access"
  role = module.patient_service_irsa.iam_role_name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"]
        Resource = ["arn:aws:s3:::${var.s3_bucket_name}", "arn:aws:s3:::${var.s3_bucket_name}/*"]
      }
    ]
  })
}

resource "aws_iam_role_policy" "patient_service_sqs" {
  name = "sqs-access"
  role = module.patient_service_irsa.iam_role_name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
      Resource = "*"
    }]
  })
}

# ── IRSA: organization-service (Cognito) ──────────────────────────────────────

module "organization_service_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name = "${var.environment}-organization-service"

  oidc_providers = {
    main = {
      provider_arn               = var.oidc_provider_arn
      namespace_service_accounts = ["patient-system:organization-service"]
    }
  }

  tags = var.tags
}

resource "aws_iam_role_policy" "organization_service_cognito" {
  name = "cognito-access"
  role = module.organization_service_irsa.iam_role_name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["cognito-idp:AdminUpdateUserAttributes", "cognito-idp:AdminGetUser"]
      Resource = "*"
    }]
  })
}
