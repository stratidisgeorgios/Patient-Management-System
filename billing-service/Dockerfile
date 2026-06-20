FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY billing-service/.mvn .mvn
COPY billing-service/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY billing-service/pom.xml billing-service/pom.xml
COPY billing-service/src billing-service/src
RUN ./mvnw package -f billing-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/billing-service/target/billing-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
