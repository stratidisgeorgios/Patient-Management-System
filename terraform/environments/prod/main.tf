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
