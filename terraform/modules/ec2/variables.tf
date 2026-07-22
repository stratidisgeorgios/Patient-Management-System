variable "instance_type" {
  description = "Type of the EC2 service"
  type        = string
}

variable "public_key" {
  description = "Public SSH key to import into AWS"
  type        = string
}

variable "environment" {
  description = "Environment that the EC2 uses"
  type        = string
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
