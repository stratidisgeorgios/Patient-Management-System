variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider for IRSA"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID — required by the ALB controller"
  type        = string
}

variable "environment" {
  description = "Deployment environment (prod, staging)"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for patientsystem.me HTTPS. Leave empty to create one via DNS validation."
  type        = string
  default     = ""
}

variable "tags" {
  type    = map(string)
  default = {}
}
