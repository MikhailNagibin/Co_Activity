FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY services/core-service/pom.xml services/core-service/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -f services/core-service/pom.xml -DskipTests dependency:go-offline

COPY services/core-service/src services/core-service/src
RUN ./mvnw -q -f services/core-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /workspace/services/core-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
