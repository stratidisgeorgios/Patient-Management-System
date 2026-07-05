terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = var.aws_region
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    cognito-idp = "http://localhost:4566"
  }
}

module "cognito" {
  source         = "../../modules/cognito"
  user_pool_name = "patient-system-${var.environment}"
  tags = {
    Environment = var.environment
    Project     = "patient-system"
  }
}
