# Build context: backend/
# Tags are pinned to a patch release; pin by digest too once you have pulled them.
FROM eclipse-temurin:21.0.8_9-jdk-noble AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --version --no-daemon
COPY model model
COPY server server
RUN ./gradlew :server:installDist --no-daemon -x test

FROM eclipse-temurin:21.0.8_9-jre-noble
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /src/server/build/install/server/ /app/
ENV PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/bin/server"]
