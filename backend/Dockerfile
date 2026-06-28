# ========== 构建阶段 ==========
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ========== 运行阶段 ==========
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
