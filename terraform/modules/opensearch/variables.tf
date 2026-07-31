variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC to place OpenSearch in"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR of the VPC — used to allow all VPC-internal traffic (EKS pods) to reach OpenSearch"
  type        = string
}

variable "subnet_ids" {
  description = "Private subnet IDs — the domain is single-node so only subnet_ids[0] is used"
  type        = list(string)
}

variable "ec2_security_group_id" {
  description = "Security group ID of the EC2 instance (allowed to reach OpenSearch)"
  type        = string
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
