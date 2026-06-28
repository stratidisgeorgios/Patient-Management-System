FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY treatment-service/.mvn .mvn
COPY treatment-service/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY treatment-service/pom.xml treatment-service/pom.xml
COPY treatment-service/src treatment-service/src
RUN ./mvnw package -f treatment-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/treatment-service/target/treatment-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
