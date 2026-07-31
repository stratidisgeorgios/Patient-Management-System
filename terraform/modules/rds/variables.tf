variable "environment" {
  description = "Environment name"
  type        = string
}

variable "db_username" {
  description = "Master username for RDS"
  type        = string
}

variable "db_password" {
  description = "Master password for RDS"
  type        = string
  sensitive   = true
}

variable "vpc_id" {
  description = "VPC to place RDS in"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR of the VPC — used to allow all VPC-internal traffic (EKS pods) to reach RDS"
  type        = string
}

variable "subnet_ids" {
  description = "Private subnet IDs for the RDS subnet group (must span at least 2 AZs)"
  type        = list(string)
}

variable "ec2_security_group_id" {
  description = "Security group ID of the EC2 instance (allowed to reach RDS)"
  type        = string
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
