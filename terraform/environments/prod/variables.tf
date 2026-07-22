variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "public_key" {
  description = "Public SSH key to import into AWS"
  type        = string
}
