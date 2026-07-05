resource "aws_cognito_user_pool" "patient_system" {
  name = var.user_pool_name

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

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
    string_attribute_constraints {
      min_length = 0
      max_length = 36
    }
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  tags = var.tags
}

resource "aws_cognito_user_pool_client" "frontend" {
  name         = "${var.user_pool_name}-client"
  user_pool_id = aws_cognito_user_pool.patient_system.id

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]

  access_token_validity  = 1
  id_token_validity      = 1
  refresh_token_validity = 30

  token_validity_units {
    access_token  = "hours"
    id_token      = "hours"
    refresh_token = "days"
  }

  prevent_user_existence_errors = "ENABLED"
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
