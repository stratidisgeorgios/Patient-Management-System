variable "environment" {
  type = string
}

variable "oidc_provider_arn" {
  description = "EKS OIDC provider ARN for IRSA"
  type        = string
}

variable "rds_address" {
  description = "RDS instance hostname"
  type        = string
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "msk_bootstrap_brokers" {
  description = "MSK plaintext bootstrap broker string"
  type        = string
}

variable "opensearch_endpoint" {
  description = "OpenSearch domain endpoint (hostname only, no https://)"
  type        = string
}

variable "cognito_user_pool_id" {
  description = "Cognito user pool ID"
  type        = string
}

variable "s3_bucket_name" {
  description = "S3 bucket name for patient file imports"
  type        = string
}

variable "import_queue_url" {
  description = "SQS import queue URL"
  type        = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
