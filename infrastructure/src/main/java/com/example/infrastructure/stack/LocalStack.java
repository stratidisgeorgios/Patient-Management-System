package com.example.infrastructure.stack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Token;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final Dotenv dotenv;

    public LocalStack(final App scope, final String id, final StackProps props, final Dotenv dotenv) {
        super(scope, id, props);
        this.dotenv = dotenv;
        this.vpc = createVpc();
        this.ecsCluster = createEcsCluster();
        DatabaseInstance authServiceDB = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDB = createDatabase("PatientServiceDB", "patient-service-db");
        CfnHealthCheck authDbHealthCheck = createHealthCheck(authServiceDB, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createHealthCheck(patientServiceDB, "PatientServiceDBHealthCheck");
        CfnCluster mskCluster = createMskCluster();
        FargateService authService = createFargateService("AuthService", "auth-service:latest", List.of(4005), authServiceDB, Map.of("JWT_SECRET", dotenv.get("JWT_SECRET")));
        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(patientDbHealthCheck);
        FargateService billingService = createFargateService("BillingService", "billing-service:latest", List.of(4001, 9001), null, null);
        FargateService analyticsService = createFargateService("AnalyticsService", "analytics-service:latest", List.of(4002), null, null);
        analyticsService.getNode().addDependency(mskCluster);
        FargateService patientService = createFargateService("PatientService", "patient-management:latest", List.of(4000), patientServiceDB, Map.of("BILLING_SERVICE_ADDRESS","host.docker.internal", "BILLING_SERVICE_GRPC_PORT","9001"));
        patientService.getNode().addDependency(patientServiceDB);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);
        createApiGatewayService();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(final String dbName, final String instanceIdentifier) {
        return DatabaseInstance.Builder.create(this, dbName)
                .instanceIdentifier(instanceIdentifier)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2)
                        .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createHealthCheck(DatabaseInstance databaseInstance, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(databaseInstance.getDbInstanceEndpointPort()))
                        .ipAddress(databaseInstance.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }

    private CfnCluster createMskCluster() {
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("3.7.x")
                .numberOfBrokerNodes(2)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.large")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(subnet -> subnet.getSubnetId())
                                .toList())
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    private FargateService createFargateService(String serviceName, String imageName, List<Integer> containerPort, DatabaseInstance databaseInstance, Map<String, String> environmentVariables) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, serviceName + "TaskDef")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");
        if (environmentVariables != null) {
            envVars.putAll(environmentVariables);
        }
        if (databaseInstance != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + databaseInstance.getDbInstanceEndpointAddress() + ":" + databaseInstance.getDbInstanceEndpointPort() + "/" + imageName + "-db");
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        Map<String, Secret> secrets = new HashMap<>();
        if (databaseInstance != null) {
            secrets.put("SPRING_DATASOURCE_PASSWORD", Secret.fromSecretsManager(databaseInstance.getSecret(), "password"));
        }

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(containerPort.stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .streamPrefix(serviceName)
                        .logGroup(LogGroup.Builder.create(this, serviceName + "LogGroup")
                                .logGroupName("/aws/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .build()))
                .environment(envVars)
                .secrets(secrets);

        taskDefinition.addContainer(serviceName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, serviceName)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .desiredCount(1)
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "ApiGatewayTaskDef")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromRegistry("api-gateway:latest"))
            .environment(Map.of(
                "SPRING_PROFILES_ACTIVE", "prod",
                "AUTH_SERVICE_URL", "http://host.docker.internal:4005",
                "PATIENT_SERVICE_URL", "http://host.docker.internal:4000",
                "BILLING_SERVICE_URL", "http://host.docker.internal:4001"
            ))
            .portMappings(List.of(4004).stream()
                    .map(port -> PortMapping.builder()
                            .containerPort(port)
                            .hostPort(port)
                            .protocol(Protocol.TCP)
                            .build())
                    .toList())
            .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                    .streamPrefix("api-gateway")
                    .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                            .logGroupName("/aws/ecs/apigateway")
                            .removalPolicy(RemovalPolicy.DESTROY)
                            .retention(RetentionDays.ONE_DAY)
                            .build())
                    .build()))
            .build();

        taskDefinition.addContainer("ApiGatewayContainer", containerOptions);
    
        ApplicationLoadBalancedFargateService apiGatewayService = ApplicationLoadBalancedFargateService.Builder.create(this, "ApiGatewayService")
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName("api-gateway")
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60))
                .build();
    }

    public static void main(final String[] args) {
        Dotenv dotenv = Dotenv.configure().directory(System.getProperty("user.dir")).load();
        App app = new App(AppProps.builder().outdir("cdk.out").build());
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "LocalStack", props, dotenv);
        app.synth();
    }
}
