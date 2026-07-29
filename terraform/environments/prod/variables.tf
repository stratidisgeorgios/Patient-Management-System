variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "public_key" {
  description = "Public SSH key to import into AWS"
  type        = string
}

variable "cognito_domain" {
  description = "Domain prefix for the Cognito hosted UI"
  type        = string
  default     = "patient-system"
}

variable "google_client_id" {
  description = "Google OAuth client ID"
  type        = string
}

variable "google_client_secret" {
  description = "Google OAuth client secret"
  type        = string
  sensitive   = true
}

variable "callback_urls" {
  description = "Allowed callback URLs after login"
  type        = list(string)
  default     = ["https://patientsystem.me/login"]
}

variable "logout_urls" {
  description = "Allowed logout URLs"
  type        = list(string)
  default     = ["https://patientsystem.me/login"]
}

variable "db_username" {
  description = "Master username for RDS PostgreSQL"
  type        = string
  default     = "admin_user"
}

variable "db_password" {
  description = "Master password for RDS PostgreSQL"
  type        = string
  sensitive   = true
}

variable "glue_scripts_bucket" {
  description = "S3 bucket for Glue scripts, jars, and temp files"
  type        = string
  default     = "patient-system-glue-scripts"
}

variable "organization_id" {
  description = "Default organization ID for Glue patient import"
  type        = string
  default     = "dbdd1a49-0881-4d95-83fa-ab2a4f740b63"
}

