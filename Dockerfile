FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG MODULE_PATH=platform-auth/auth-web
ARG JAR_PATH=platform-auth/auth-web/target/auth-web-0.1.0-SNAPSHOT.jar

WORKDIR /workspace
COPY . .
RUN mvn -q -pl "${MODULE_PATH}" -am package -DskipTests \
    && cp "${JAR_PATH}" /tmp/app.jar

FROM eclipse-temurin:21-jre-alpine

ARG APP_PORT=8081

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/logs \
    && chown -R app:app /app/logs
COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE ${APP_PORT}
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
