FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG MODULE_PATH=platform-blog/upstream/nblog/blog-api
ARG JAR_PATH=platform-blog/upstream/nblog/blog-api/target/blog-api-0.0.1.jar

WORKDIR /workspace
COPY . .
RUN mvn -q -pl "${MODULE_PATH}" -am package -DskipTests \
    && cp "${JAR_PATH}" /tmp/app.jar

FROM eclipse-temurin:21-jre-alpine

ARG APP_PORT=8090

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/logs \
    && chown -R app:app /app/logs
COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE ${APP_PORT}
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
