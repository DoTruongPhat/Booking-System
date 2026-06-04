# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source và build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user non-root để bảo mật
RUN addgroup -S booking && adduser -S booking -G booking

# Copy JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Tạo folder logs + đổi owner toàn bộ /app
RUN mkdir -p /app/logs && chown -R booking:booking /app

# Chạy với user non-root
USER booking

# JVM tuning
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]