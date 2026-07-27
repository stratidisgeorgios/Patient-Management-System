import sys
import uuid
import json
from datetime import datetime, date

from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from pyspark.sql import functions as F
from pyspark.sql.types import StringType, DateType, StructType, StructField

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

# Read CSV — expected columns: name,email,gender,address,date_of_birth,registered_date
df = spark.read.option("header", "true").option("inferSchema", "false").csv(s3_path)

# Assign UUIDs and organization_id
udf_uuid = F.udf(lambda: str(uuid.uuid4()), StringType())

df = (df
      .withColumn("id", udf_uuid())
      .withColumn("organization_id", F.lit(organization_id))
      .withColumn("date_of_birth", F.to_date(F.col("date_of_birth"), "yyyy-MM-dd"))
      .withColumn("registered_date", F.to_date(F.col("registered_date"), "yyyy-MM-dd"))
      .withColumn("custom_fields", F.lit(None).cast(StringType()))
      .select("id", "name", "email", "gender", "address",
              "date_of_birth", "registered_date", "organization_id", "custom_fields"))

jdbc_props = {
    "user": args["RDS_USER"],
    "password": args["RDS_PASSWORD"],
    "driver": "org.postgresql.Driver",
    "batchsize": "5000",
    "reWriteBatchedInserts": "true",
}

df.write \
    .mode("append") \
    .jdbc(url=jdbc_url, table="patient", properties=jdbc_props)

# Publish a single summary event to Kafka so the app knows the import finished
total = df.count()
summary = json.dumps({
    "organization_id": organization_id,
    "total_imported": total,
    "finished_at": datetime.utcnow().isoformat(),
})

event_df = spark.createDataFrame(
    [(organization_id, summary)],
    schema=StructType([
        StructField("key", StringType()),
        StructField("value", StringType()),
    ])
)

event_df.selectExpr("key", "value") \
    .write \
    .format("kafka") \
    .option("kafka.bootstrap.servers", args["KAFKA_BROKERS"]) \
    .option("topic", "patient-import-events") \
    .save()

print(f"Import complete: {total} patients written for organization {organization_id}")
job.commit()
