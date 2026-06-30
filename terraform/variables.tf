variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-west-2"
}

variable "app_name" {
  description = "Name used to tag and namespace resources"
  type        = string
  default     = "url-shortener"
}

variable "db_username" {
  type    = string
  default = "postgres"
}

variable "db_password" {
  description = "Postgres master password — pass via TF_VAR_db_password, never commit it"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "Full image reference, e.g. ghcr.io/fikratdev/url-shortener:latest"
  type        = string
}

variable "container_port" {
  type    = number
  default = 8080
}
