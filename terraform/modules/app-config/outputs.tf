output "secret_arn" {
  description = "AWS Secrets Manager secret ARN containing all runtime config"
  value       = aws_secretsmanager_secret.patient_system.arn
}

output "patient_service_role_arn" {
  description = "IRSA role for patient-service (S3 + SQS)"
  value       = module.patient_service_irsa.iam_role_arn
}

output "organization_service_role_arn" {
  description = "IRSA role for organization-service (Cognito)"
  value       = module.organization_service_irsa.iam_role_arn
}

output "eso_role_arn" {
  description = "IRSA role for External Secrets Operator"
  value       = aws_iam_role.eso.arn
}
