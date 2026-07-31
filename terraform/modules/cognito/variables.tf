variable "user_pool_name" {
  description = "Name of the Cognito User Pool"
  type        = string
}

variable "cognito_domain" {
  description = "Domain prefix for the Cognito hosted UI"
  type        = string
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
}

variable "logout_urls" {
  description = "Allowed logout URLs"
  type        = list(string)
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

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
