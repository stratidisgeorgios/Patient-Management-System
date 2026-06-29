FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY patient-service/.mvn .mvn
COPY patient-service/mvnw .

COPY pom.xml .
RUN ./mvnw install -N

COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN ./mvnw package -f api-gateway/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
