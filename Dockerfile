FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 10001 procureflow \
    && useradd --system --uid 10001 --gid procureflow --home-dir /app --shell /usr/sbin/nologin procureflow
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/app-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chown=10001:10001 --chmod=0555 docker-entrypoint.sh /app/docker-entrypoint.sh
ENV SPRING_PROFILES_ACTIVE=deploy
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m"
EXPOSE 10000
USER 10001:10001
ENTRYPOINT ["/app/docker-entrypoint.sh"]
