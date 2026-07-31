variable "environment" {
  type = string
}

variable "grafana_admin_password" {
  description = "Grafana admin password — store in terraform.tfvars (gitignored)"
  type        = string
  sensitive   = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
