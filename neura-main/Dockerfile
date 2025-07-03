
FROM openjdk:21-jdk-slim AS builder

ARG GITHUB_PACKAGES_TOKEN

WORKDIR /app
COPY . .

ENV GITHUB_PACKAGES_TOKEN $GITHUB_PACKAGES_TOKEN
RUN ./gradlew build -x test

ENV GITHUB_PACKAGES_TOKEN ""

FROM openjdk:21-jdk-slim

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
