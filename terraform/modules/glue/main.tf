data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_subnet" "glue" {
  id = tolist(data.aws_subnets.default.ids)[0]
}

data "aws_route_tables" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = data.aws_vpc.default.id
  service_name      = "com.amazonaws.${data.aws_region.current.name}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = data.aws_route_tables.default.ids
}

resource "aws_security_group" "glue" {
  name        = "patient-system-${var.environment}-glue-sg"
  description = "Glue job - self-referencing plus RDS and MSK access"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Self-referencing required by Glue"
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

resource "aws_iam_role" "glue" {
  name = "patient-system-${var.environment}-glue-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "glue.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "glue_service" {
  role       = aws_iam_role.glue.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
}

resource "aws_iam_role_policy" "glue_s3" {
  name = "glue-s3-access"
  role = aws_iam_role.glue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"]
      Resource = ["arn:aws:s3:::${var.glue_scripts_bucket}", "arn:aws:s3:::${var.glue_scripts_bucket}/*",
                  "arn:aws:s3:::${var.patient_csv_bucket}", "arn:aws:s3:::${var.patient_csv_bucket}/*"]
    }]
  })
}

resource "aws_glue_connection" "rds" {
  name            = "patient-system-${var.environment}-rds"
  connection_type = "NETWORK"

  physical_connection_requirements {
    availability_zone      = data.aws_subnet.glue.availability_zone
    subnet_id              = data.aws_subnet.glue.id
    security_group_id_list = [aws_security_group.glue.id]
  }
}

resource "aws_s3_object" "glue_script" {
  bucket = var.glue_scripts_bucket
  key    = "scripts/import_patients.py"
  source = "${path.module}/../../../glue/import_patients.py"
  etag   = filemd5("${path.module}/../../../glue/import_patients.py")
}

resource "aws_glue_job" "import_patients" {
  name         = "patient-system-${var.environment}-import-patients"
  role_arn     = aws_iam_role.glue.arn
  glue_version = "4.0"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${var.glue_scripts_bucket}/scripts/import_patients.py"
  }

  default_arguments = {
    "--job-language"                     = "python"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-metrics"                   = "true"
    "--TempDir"                          = "s3://${var.glue_scripts_bucket}/temp/"
    "--extra-jars"                       = "s3://${var.glue_scripts_bucket}/jars/postgresql-42.7.3.jar"
    "--additional-python-modules"        = "s3://${var.glue_scripts_bucket}/jars/kafka_python-2.0.2-py2.py3-none-any.whl"
    "--RDS_ENDPOINT"                     = var.rds_endpoint
    "--RDS_PORT"                         = "5432"
    "--RDS_DB"                           = "patient_db"
    "--RDS_USER"                         = var.db_username
    "--RDS_PASSWORD"                     = var.db_password
    "--S3_INPUT_BUCKET"                  = var.patient_csv_bucket
    "--S3_INPUT_KEY"                     = "patients_import.csv"
    "--KAFKA_BROKERS"                    = var.kafka_bootstrap_brokers
    "--ORGANIZATION_ID"                  = var.organization_id
  }

  connections = [aws_glue_connection.rds.name]

  number_of_workers = 10
  worker_type       = "G.1X"

  execution_property {
    max_concurrent_runs = 1
  }

  tags = var.tags
}
