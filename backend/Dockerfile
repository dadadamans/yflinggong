#FROM maven:3.9.9-eclipse-temurin-17 AS builder
#
#WORKDIR /app
#
#COPY pom.xml .
#COPY src ./src
#
#RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre AS builder

WORKDIR /app

COPY --from=builder /app/target/silver-job-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
