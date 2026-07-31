variable "environment" {
  description = "Environment name"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.31"
}

variable "vpc_id" {
  description = "VPC to deploy the cluster into"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnets for the worker nodes (one per AZ)"
  type        = list(string)
}

variable "admin_role_arns" {
  description = "IAM role ARNs to grant EKS cluster-admin access (e.g. CI role)"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
