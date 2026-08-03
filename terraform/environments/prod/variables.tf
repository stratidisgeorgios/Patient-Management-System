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

variable "e2e_test_email" {
  description = "Email for the E2E Cognito test user"
  type        = string
}

variable "e2e_test_password" {
  description = "Password for the E2E Cognito test user"
  type        = string
  sensitive   = true
}

variable "grafana_admin_password" {
  description = "Grafana admin password"
  type        = string
  sensitive   = true
}

variable "argocd_admin_password_bcrypt" {
  description = "bcrypt hash of ArgoCD admin password (htpasswd -nbBC 10 '' PASSWORD | tr -d ':')"
  type        = string
  sensitive   = true
}

variable "acm_certificate_arn" {
  description = "Existing ACM certificate ARN for patientsystem.me. Leave empty to create one."
  type        = string
  default     = ""
}

