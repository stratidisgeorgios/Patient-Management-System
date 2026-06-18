FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY patient-management/.mvn .mvn
COPY patient-management/mvnw .

COPY pom.xml .
COPY proto ./proto
RUN ./mvnw install -N && ./mvnw install -f proto/pom.xml

COPY patient-management/pom.xml patient-management/pom.xml
COPY patient-management/src patient-management/src
RUN ./mvnw package -f patient-management/pom.xml -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/patient-management/target/patient-management-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]
