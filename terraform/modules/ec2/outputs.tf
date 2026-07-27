output "public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.main.public_ip
}

output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.main.id
}

output "security_group_id" {
  description = "Security group ID of the EC2 instance"
  value       = aws_security_group.main.id
}
