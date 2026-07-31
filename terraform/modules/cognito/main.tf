resource "aws_cognito_user_pool" "patient_system" {
  name = var.user_pool_name

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  deletion_protection      = "INACTIVE"
  mfa_configuration        = "OFF"

  password_policy {
    minimum_length                   = 8
    require_lowercase                = true
    require_uppercase                = true
    require_numbers                  = true
    require_symbols                  = false
    temporary_password_validity_days = 7
  }

  schema {
    name                     = "organizationId"
    attribute_data_type      = "String"
    mutable                  = true
    required                 = false
    developer_only_attribute = false
    string_attribute_constraints {
      min_length = "0"
      max_length = "36"
    }
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  email_configuration {
    email_sending_account = "COGNITO_DEFAULT"
  }

  user_attribute_update_settings {
    attributes_require_verification_before_update = []
  }

  tags = var.tags
}

resource "aws_cognito_user_pool_domain" "main" {
  domain       = var.cognito_domain
  user_pool_id = aws_cognito_user_pool.patient_system.id
}

resource "aws_cognito_identity_provider" "google" {
  user_pool_id  = aws_cognito_user_pool.patient_system.id
  provider_name = "Google"
  provider_type = "Google"

  provider_details = {
    client_id             = var.google_client_id
    client_secret         = var.google_client_secret
    authorize_scopes      = "email profile openid"
    authorize_url         = "https://accounts.google.com/o/oauth2/v2/auth"
    token_url             = "https://www.googleapis.com/oauth2/v4/token"
    token_request_method  = "POST"
    oidc_issuer           = "https://accounts.google.com"
    attributes_url        = "https://people.googleapis.com/v1/people/me?personFields="
    attributes_url_add_attributes = "true"
  }

  attribute_mapping = {
    username = "sub"
  }
}

resource "aws_cognito_user_pool_client" "frontend" {
  name         = "${var.user_pool_name}-client"
  user_pool_id = aws_cognito_user_pool.patient_system.id

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]

  supported_identity_providers         = ["COGNITO", "Google"]
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  callback_urls                        = var.callback_urls
  logout_urls                          = var.logout_urls

  access_token_validity  = 1
  id_token_validity      = 1
  refresh_token_validity = 30
  auth_session_validity  = 3

  token_validity_units {
    access_token  = "hours"
    id_token      = "hours"
    refresh_token = "days"
  }

  enable_token_revocation               = true
  prevent_user_existence_errors         = "ENABLED"
  enable_propagate_additional_user_context_data = false

  depends_on = [aws_cognito_identity_provider.google]
}

resource "aws_cognito_user" "e2e_test" {
  user_pool_id   = aws_cognito_user_pool.patient_system.id
  username       = var.e2e_test_email
  message_action = "SUPPRESS"

  attributes = {
    email          = var.e2e_test_email
    email_verified = "true"
  }
}

resource "aws_cognito_user_password" "e2e_test" {
  user_pool_id = aws_cognito_user_pool.patient_system.id
  username     = aws_cognito_user.e2e_test.username
  password     = var.e2e_test_password
  permanent    = true
}

resource "aws_cognito_user_group" "admin" {
  name         = "Admin"
  user_pool_id = aws_cognito_user_pool.patient_system.id
  description  = "Tenant administrators"
}

resource "aws_cognito_user_group" "doctor" {
  name         = "Doctor"
  user_pool_id = aws_cognito_user_pool.patient_system.id
  description  = "Doctors with full patient access"
}

resource "aws_cognito_user_group" "nurse" {
  name         = "Nurse"
  user_pool_id = aws_cognito_user_pool.patient_system.id
  description  = "Nurses with patient and treatment access"
}

resource "aws_cognito_user_group" "secretary" {
  name         = "Secretary"
  user_pool_id = aws_cognito_user_pool.patient_system.id
  description  = "Secretaries with billing access"
}
