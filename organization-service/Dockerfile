FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY organization-service/.mvn .mvn
COPY organization-service/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY organization-service/pom.xml organization-service/pom.xml
COPY organization-service/src organization-service/src
RUN ./mvnw package -f organization-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/organization-service/target/organizationservice-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
