import sys
import uuid
import json
from datetime import datetime
from kafka import KafkaProducer

from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from pyspark.sql import functions as F
from pyspark.sql.types import StringType, StructType, StructField

args = getResolvedOptions(sys.argv, [
    "JOB_NAME",
    "RDS_ENDPOINT",
    "RDS_PORT",
    "RDS_DB",
    "RDS_USER",
    "RDS_PASSWORD",
    "S3_INPUT_BUCKET",
    "S3_INPUT_KEY",
    "KAFKA_BROKERS",
    "ORGANIZATION_ID",
])

sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args["JOB_NAME"], args)

organization_id = args["ORGANIZATION_ID"]
jdbc_url = f"jdbc:postgresql://{args['RDS_ENDPOINT']}:{args['RDS_PORT']}/{args['RDS_DB']}"
s3_path = f"s3://{args['S3_INPUT_BUCKET']}/{args['S3_INPUT_KEY']}"

# Read CSV — columns: name,email,gender,address,dateOfBirth,registeredDate + extras go to custom_fields
df = spark.read.option("header", "true").option("inferSchema", "false").csv(s3_path)

udf_uuid = F.udf(lambda: str(uuid.uuid4()), StringType())

# Extra columns not in the core schema go into custom_fields as JSON
core_cols = {"name", "email", "gender", "address", "dateOfBirth", "registeredDate", "id", "organization_id"}
extra_cols = [c for c in df.columns if c not in core_cols]

udf_custom = F.udf(
    lambda *vals: json.dumps({k: v for k, v in zip(extra_cols, vals) if v is not None}),
    StringType()
)

df = (df
      .withColumn("id", udf_uuid())
      .withColumn("organization_id", F.lit(organization_id))
      .withColumn("date_of_birth", F.to_date(F.col("dateOfBirth"), "yyyy-MM-dd"))
      .withColumn("registered_date", F.to_date(F.col("registeredDate"), "yyyy-MM-dd"))
      .withColumn("custom_fields", udf_custom(*[F.col(c) for c in extra_cols]))
      .select("id", "name", "email", "gender", "address",
              "date_of_birth", "registered_date", "organization_id", "custom_fields"))

jdbc_props = {
    "user": args["RDS_USER"],
    "password": args["RDS_PASSWORD"],
    "driver": "org.postgresql.Driver",
    "batchsize": "5000",
    "reWriteBatchedInserts": "true",
    "stringtype": "unspecified",
}

df.write \
    .mode("append") \
    .jdbc(url=jdbc_url, table="patient", properties=jdbc_props)

total = df.count()
print(f"Import complete: {total} patients written for organization {organization_id}")

summary = json.dumps({
    "organization_id": organization_id,
    "total_imported": total,
    "finished_at": datetime.utcnow().isoformat(),
})
producer = KafkaProducer(bootstrap_servers=args["KAFKA_BROKERS"].split(","))
producer.send("patient-import-events", key=organization_id.encode(), value=summary.encode())
producer.flush()
producer.close()
job.commit()
