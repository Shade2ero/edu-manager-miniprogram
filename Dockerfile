# ========== 微信云托管 Dockerfile ==========
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY init.sql /docker-entrypoint-initdb.d/
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]
