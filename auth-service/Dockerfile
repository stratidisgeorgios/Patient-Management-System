FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY auth-service/.mvn .mvn
COPY auth-service/mvnw .

COPY pom.xml .
RUN ./mvnw install -N

COPY auth-service/pom.xml auth-service/pom.xml
COPY auth-service/src auth-service/src
RUN ./mvnw package -f auth-service/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 4005
ENTRYPOINT ["java", "-jar", "app.jar"]
