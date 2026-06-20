FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY patient-management/.mvn .mvn
COPY patient-management/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY analytics-service/pom.xml analytics-service/pom.xml
COPY analytics-service/src analytics-service/src
RUN ./mvnw package -f analytics-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/analytics-service/target/analytics-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
