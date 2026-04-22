# 运行阶段镜像：不在 Docker 内执行 Maven，只负责启动宿主机已打包好的 jar。
FROM eclipse-temurin:17-jre

WORKDIR /app

# 这里依赖宿主机提前执行 mvn clean package -DskipTests 生成目标 jar。
COPY target/silver-job-backend-1.0.0.jar app.jar

EXPOSE 8080

# 直接启动 Spring Boot 可执行 jar。
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
