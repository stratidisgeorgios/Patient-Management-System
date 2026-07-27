output "glue_job_name" {
  value = aws_glue_job.import_patients.name
}

output "glue_security_group_id" {
  value = aws_security_group.glue.id
}

output "glue_role_arn" {
  value = aws_iam_role.glue.arn
}
