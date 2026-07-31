terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Derived from environment so every module that needs the cluster name
# (VPC tags, EKS, IRSA roles) always uses the same value
locals {
  cluster_name = "${var.environment}-patient-system"
}

# ────────────────────────────────────────────────────────────
# Networking
# ────────────────────────────────────────────────────────────

module "vpc" {
  source = "../../modules/vpc"

  environment  = var.environment
  cluster_name = local.cluster_name

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

# ────────────────────────────────────────────────────────────
# Kubernetes Cluster
# ────────────────────────────────────────────────────────────

module "eks" {
  source = "../../modules/eks"

  environment        = var.environment
  cluster_name       = local.cluster_name
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "eks_cluster_name" {
  description = "Run: aws eks update-kubeconfig --region eu-west-1 --name <value>"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "eks_oidc_provider_arn" {
  description = "Used when creating IRSA IAM roles for pods"
  value       = module.eks.oidc_provider_arn
}

output "eks_node_security_group_id" {
  description = "Reference this in Phase 4 to tighten RDS/MSK/OpenSearch SG ingress to EKS nodes only"
  value       = module.eks.node_security_group_id
}

# ────────────────────────────────────────────────────────────
# Container Registry + GitHub Actions OIDC
# ────────────────────────────────────────────────────────────

module "ecr" {
  source      = "../../modules/ecr"
  environment = var.environment

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "ecr_repository_urls" {
  description = "Push images to these URLs from GitHub Actions"
  value       = module.ecr.repository_urls
}

output "github_actions_role_arn" {
  description = "Use in GitHub Actions: role-to-assume"
  value       = module.ecr.github_actions_role_arn
}

# ────────────────────────────────────────────────────────────
# Auth
# ────────────────────────────────────────────────────────────

module "cognito" {
  source               = "../../modules/cognito"
  user_pool_name       = "patient-system-${var.environment}"
  cognito_domain       = var.cognito_domain
  google_client_id     = var.google_client_id
  google_client_secret = var.google_client_secret
  callback_urls        = var.callback_urls
  logout_urls          = var.logout_urls
  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "user_pool_id" {
  value = module.cognito.user_pool_id
}

output "client_id" {
  value = module.cognito.client_id
}

output "jwks_uri" {
  value = module.cognito.jwks_uri
}

# ────────────────────────────────────────────────────────────
# EC2 (Docker Compose server — kept as bastion during migration)
# ────────────────────────────────────────────────────────────

module "ec2" {
  source        = "../../modules/ec2"
  instance_type = "t3.large"
  public_key    = var.public_key
  environment   = var.environment
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.public_subnet_ids[0]
  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "ec2_public_ip" {
  value = module.ec2.public_ip
}

output "ec2_instance_id" {
  value = module.ec2.instance_id
}

# ────────────────────────────────────────────────────────────
# Storage
# ────────────────────────────────────────────────────────────

module "s3" {
  source      = "../../modules/s3"
  bucket_name = "patient-system-s3-storage-jjfd3rf"
  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "s3_bucket_name" {
  value = module.s3.bucket_name
}

module "sqs_import" {
  source     = "../../modules/sqs"
  queue_name = "patient-system-import-queue"
  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "import_queue_url" {
  value = module.sqs_import.queue_url
}

# ────────────────────────────────────────────────────────────
# Data layer (all in private subnets)
# ────────────────────────────────────────────────────────────

module "rds" {
  source = "../../modules/rds"

  environment           = var.environment
  db_username           = var.db_username
  db_password           = var.db_password
  vpc_id                = module.vpc.vpc_id
  vpc_cidr              = module.vpc.vpc_cidr
  subnet_ids            = module.vpc.private_subnet_ids
  ec2_security_group_id = module.ec2.security_group_id

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_address" {
  value = module.rds.address
}

module "msk" {
  source = "../../modules/msk"

  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  vpc_cidr              = module.vpc.vpc_cidr
  subnet_ids            = module.vpc.private_subnet_ids
  ec2_security_group_id = module.ec2.security_group_id

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "msk_bootstrap_brokers" {
  value = module.msk.bootstrap_brokers
}

module "opensearch" {
  source = "../../modules/opensearch"

  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  vpc_cidr              = module.vpc.vpc_cidr
  subnet_ids            = module.vpc.private_subnet_ids
  ec2_security_group_id = module.ec2.security_group_id

  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}

output "opensearch_endpoint" {
  value = module.opensearch.endpoint
}
