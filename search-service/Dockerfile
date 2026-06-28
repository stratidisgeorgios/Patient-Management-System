FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY search-service/.mvn .mvn
COPY search-service/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY search-service/pom.xml search-service/pom.xml
COPY search-service/src search-service/src
RUN ./mvnw package -f search-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/search-service/target/search-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
