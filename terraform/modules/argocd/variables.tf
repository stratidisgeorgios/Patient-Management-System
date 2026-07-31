variable "environment" {
  type = string
}

variable "argocd_admin_password_bcrypt" {
  description = "bcrypt hash of ArgoCD admin password. Generate with: htpasswd -nbBC 10 '' PASSWORD | tr -d ':'"
  type        = string
  sensitive   = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
