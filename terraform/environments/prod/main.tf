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

module "cognito" {
  source         = "../../modules/cognito"
  user_pool_name = "patient-system-${var.environment}"
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

module "ec2" {
  source        = "../../modules/ec2"
  instance_type = "t3.large"
  public_key    = var.public_key
  environment   = var.environment
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
