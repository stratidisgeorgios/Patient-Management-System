variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC to place MSK in"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR of the VPC — used to allow all VPC-internal traffic (EKS pods) to reach Kafka"
  type        = string
}

variable "subnet_ids" {
  description = "Private subnet IDs for MSK broker nodes (one subnet per broker, each in a different AZ)"
  type        = list(string)
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
