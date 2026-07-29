variable "environment" {
  type = string
}

variable "rds_endpoint" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "glue_scripts_bucket" {
  type        = string
  description = "S3 bucket name for Glue scripts and jars"
}

variable "patient_csv_bucket" {
  type        = string
  description = "S3 bucket name where the patient CSV is uploaded"
}

variable "kafka_bootstrap_brokers" {
  type = string
}

variable "organization_id" {
  type        = string
  description = "Default organization ID to import patients into"
  default     = ""
}

variable "tags" {
  type    = map(string)
  default = {}
}
