resource "aws_security_group" "msk" {
  name        = "patient-system-${var.environment}-msk-sg"
  description = "Allow Kafka from EC2 and EKS pods"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Kafka from EC2"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [var.ec2_security_group_id]
  }

  # All resources inside the VPC (EKS pods, etc.)
  ingress {
    description = "Kafka from within VPC"
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # MSK brokers need to talk to each other
  ingress {
    description = "Broker-to-broker communication"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = var.tags
}

resource "aws_msk_configuration" "main" {
  name           = "patient-system-${var.environment}"
  kafka_versions = ["3.6.0"]
  server_properties = <<-EOF
    auto.create.topics.enable=true
    default.replication.factor=2
    min.insync.replicas=1
    num.partitions=3
  EOF
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "patient-system-${var.environment}"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 2

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    client_subnets  = var.subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 20
      }
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.main.arn
    revision = aws_msk_configuration.main.latest_revision
  }

  client_authentication {
    unauthenticated = true
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "PLAINTEXT"
      in_cluster    = true
    }
  }

  tags = var.tags
}
