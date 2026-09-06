FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S quespot && adduser -S quespot -G quespot

WORKDIR /app

COPY --from=builder --chown=quespot:quespot /workspace/build/libs/*.jar app.jar

USER quespot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
