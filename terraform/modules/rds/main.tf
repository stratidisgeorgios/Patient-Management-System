resource "aws_security_group" "rds" {
  name        = "patient-system-${var.environment}-rds-sg"
  description = "Allow PostgreSQL from EC2 and EKS pods"
  vpc_id      = var.vpc_id

  ingress {
    description = "PostgreSQL from within VPC (EKS pods)"
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
  engine_version    = "16.13"
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
