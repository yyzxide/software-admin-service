FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY admin-service/pom.xml admin-service/pom.xml
COPY admin-service/src admin-service/src
RUN cd admin-service && mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app/admin-service
COPY --from=build /workspace/admin-service/target/admin-service-1.0.0.jar ./admin-service.jar
COPY admin-ui /app/admin-ui

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/admin-service/admin-service.jar"]
