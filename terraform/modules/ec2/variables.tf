variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "public_key" {
  description = "Public SSH key to import into AWS"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC to place the EC2 instance and its security group in"
  type        = string
}

variable "subnet_id" {
  description = "Public subnet to launch the EC2 instance into"
  type        = string
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
