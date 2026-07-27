variable "environment" {
  type = string
}

variable "ec2_security_group_id" {
  type = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
