# Uses the battle-tested community EKS module.
# It creates: the control plane, IAM roles for the cluster and nodes,
# managed node groups, security groups, and the OIDC provider for IRSA.
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.cluster_name
  cluster_version = var.cluster_version

  vpc_id     = var.vpc_id
  subnet_ids = var.private_subnet_ids

  # Expose the API server endpoint publicly so we can run kubectl
  # from a laptop or CI runner without needing a VPN.
  # In production you'd restrict this to specific CIDR blocks.
  cluster_endpoint_public_access = true

  # Grant the IAM identity running terraform full admin access to the cluster.
  # Without this the cluster creator has no k8s permissions after creation.
  enable_cluster_creator_admin_permissions = true

  # Disabled: requires kms:TagResource which the terraform IAM user lacks.
  # In production you'd enable this to encrypt Kubernetes Secrets at rest.
  create_kms_key            = false
  cluster_encryption_config = {}

  cluster_enabled_log_types = ["api", "audit", "authenticator"]

  access_entries = {
    for arn in var.admin_role_arns : arn => {
      principal_arn = arn
      policy_associations = {
        admin = {
          policy_arn   = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = { type = "cluster" }
        }
      }
    }
  }

  eks_managed_node_groups = {
    main = {
      instance_types = ["t3.medium"]

      # SPOT saves ~70% over on-demand. Acceptable for a demo because:
      # - interruptions are rare for t3.medium (low-demand instance)
      # - desired=2 means a single interruption doesn't take everything down
      capacity_type = "SPOT"

      min_size     = 1
      max_size     = 4
      desired_size = 3

      labels = {
        Environment = var.environment
      }
    }
  }

  tags = var.tags
}

locals {
  # Strip "https://" so the OIDC URL can be used as a condition key
  oidc_issuer = trimprefix(module.eks.cluster_oidc_issuer_url, "https://")
}

data "aws_iam_policy_document" "ebs_csi_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer}:sub"
      values   = ["system:serviceaccount:kube-system:ebs-csi-controller-sa"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ebs_csi" {
  name               = "${var.cluster_name}-ebs-csi-driver"
  assume_role_policy = data.aws_iam_policy_document.ebs_csi_assume_role.json
  tags               = var.tags
}

resource "aws_iam_role_policy_attachment" "ebs_csi" {
  role       = aws_iam_role.ebs_csi.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

resource "aws_eks_addon" "ebs_csi" {
  cluster_name             = module.eks.cluster_name
  addon_name               = "aws-ebs-csi-driver"
  service_account_role_arn = aws_iam_role.ebs_csi.arn
  depends_on               = [module.eks]
  tags                     = var.tags
}
