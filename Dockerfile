FROM eclipse-temurin:17-jdk-jammy AS builder

ARG STATIC_PATH=src/main/resources/static
ARG API_DOCS=api-docs.yaml

WORKDIR /project

COPY . .

RUN chmod +x gradlew && \
    mkdir -p "$STATIC_PATH" && \
    mv "$API_DOCS" "$STATIC_PATH" && \
    ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /webapp

RUN groupadd -r app && \
    useradd -r -g app -d /webapp -s /bin/false app && \
    mkdir -p image log && \
    chown -R app:app /webapp

COPY --from=builder --chown=app:app /project/build/libs/*.jar app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
