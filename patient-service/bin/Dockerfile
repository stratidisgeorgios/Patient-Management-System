FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY patient-service/.mvn .mvn
COPY patient-service/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY patient-service/pom.xml patient-service/pom.xml
COPY patient-service/src patient-service/src
RUN ./mvnw package -f patient-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/patient-service/target/patient-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
