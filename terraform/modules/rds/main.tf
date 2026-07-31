resource "aws_security_group" "rds" {
  name        = "patient-system-${var.environment}-rds-sg"
  description = "Allow PostgreSQL from EC2 and EKS pods"
  vpc_id      = var.vpc_id

  # Existing EC2 instance (Docker Compose era, kept as bastion during migration)
  ingress {
    description     = "PostgreSQL from EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ec2_security_group_id]
  }

  # All resources inside the VPC (EKS pods, etc.)
  # More granular: in Phase 4 we'll narrow this to the EKS node security group
  ingress {
    description = "PostgreSQL from within VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = var.tags
}

resource "aws_db_subnet_group" "main" {
  name       = "patient-system-${var.environment}-subnet-group"
  subnet_ids = var.subnet_ids
  tags       = var.tags
}

resource "aws_db_instance" "main" {
  identifier        = "patient-system-${var.environment}"
  engine            = "postgres"
  engine_version    = "16.9"
  instance_class    = "db.t3.medium"
  allocated_storage = 100
  storage_type      = "gp2"

  db_name  = "patient_db"
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible     = false
  skip_final_snapshot     = true
  deletion_protection     = false
  backup_retention_period = 0

  tags = var.tags
}
