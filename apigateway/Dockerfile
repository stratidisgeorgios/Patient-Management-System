FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY patient-management/.mvn .mvn
COPY patient-management/mvnw .

COPY pom.xml .
RUN ./mvnw install -N

COPY apigateway/pom.xml apigateway/pom.xml
COPY apigateway/src apigateway/src
RUN ./mvnw package -f apigateway/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/apigateway/target/apigateway-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
