FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y tzdata && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/admin-service-*.jar admin-service.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "admin-service.jar", "--spring.config.additional-location=optional:file:/app/config/application.yml"]
