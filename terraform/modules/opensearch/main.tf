resource "aws_iam_service_linked_role" "opensearch" {
  aws_service_name = "es.amazonaws.com"
}

resource "aws_security_group" "opensearch" {
  name        = "patient-system-${var.environment}-opensearch-sg"
  description = "Allow HTTPS to OpenSearch from EC2 and EKS pods"
  vpc_id      = var.vpc_id

  ingress {
    description     = "HTTPS from EC2"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [var.ec2_security_group_id]
  }

  # All resources inside the VPC (EKS pods, etc.)
  ingress {
    description = "HTTPS from within VPC"
    from_port   = 443
    to_port     = 443
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

resource "aws_opensearch_domain" "main" {
  depends_on = [aws_iam_service_linked_role.opensearch]

  domain_name    = "patient-system-${var.environment}"
  engine_version = "OpenSearch_2.11"

  cluster_config {
    instance_type          = "t3.small.search"
    instance_count         = 1
    zone_awareness_enabled = false
  }

  ebs_options {
    ebs_enabled = true
    volume_size = 20
    volume_type = "gp2"
  }

  vpc_options {
    # Single-node OpenSearch only uses one subnet
    subnet_ids         = [var.subnet_ids[0]]
    security_group_ids = [aws_security_group.opensearch.id]
  }

  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https = true
  }

  access_policies = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { AWS = "*" }
      Action    = "es:*"
      Resource  = "arn:aws:es:*:*:domain/patient-system-${var.environment}/*"
    }]
  })

  tags = var.tags
}
