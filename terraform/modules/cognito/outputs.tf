output "user_pool_id" {
  value = aws_cognito_user_pool.patient_system.id
}

output "user_pool_arn" {
  value = aws_cognito_user_pool.patient_system.arn
}

output "client_id" {
  value = aws_cognito_user_pool_client.frontend.id
}

output "jwks_uri" {
  value = "https://cognito-idp.${data.aws_region.current.name}.amazonaws.com/${aws_cognito_user_pool.patient_system.id}/.well-known/jwks.json"
}

data "aws_region" "current" {}
