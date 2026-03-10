FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY services/notifications-service/pom.xml services/notifications-service/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -f services/notifications-service/pom.xml -DskipTests dependency:go-offline

COPY services/notifications-service/src services/notifications-service/src
RUN ./mvnw -q -f services/notifications-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /workspace/services/notifications-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
