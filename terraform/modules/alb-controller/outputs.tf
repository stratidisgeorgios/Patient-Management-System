output "lb_controller_role_arn" {
  description = "IAM role ARN used by the ALB controller pod"
  value       = module.lb_controller_irsa.iam_role_arn
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN to paste into k8s/ingress/ingress.yaml"
  value       = local.certificate_arn
}

output "acm_certificate_domain_validation_options" {
  description = "DNS CNAME records to add in Cloudflare to validate the ACM certificate"
  value = length(aws_acm_certificate.patient_system) > 0 ? (
    aws_acm_certificate.patient_system[0].domain_validation_options
  ) : toset([])
}
