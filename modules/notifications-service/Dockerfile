FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY modules/notifications-service/pom.xml modules/notifications-service/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -f modules/notifications-service/pom.xml -DskipTests dependency:go-offline

COPY modules/notifications-service/src modules/notifications-service/src
RUN ./mvnw -q -f modules/notifications-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /workspace/modules/notifications-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
