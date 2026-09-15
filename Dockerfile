# Bước 1: Build source code bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng bằng JRE gọn nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render sẽ tự cấp biến môi trường PORT (thường là 10000), ánh xạ cổng vào Spring Boot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]