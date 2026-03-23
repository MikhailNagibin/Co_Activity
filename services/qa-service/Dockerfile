FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY services/qa-service/pom.xml services/qa-service/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -f services/qa-service/pom.xml -DskipTests dependency:go-offline

COPY services/qa-service/src services/qa-service/src
RUN ./mvnw -q -f services/qa-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /workspace/services/qa-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
