FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
