FROM eclipse-temurin:21-jre-jammy@sha256:8d584faeef02c22f6b58ad8a3cee8e1e53ae52bdf2224f4afb989cc35983b8ae
WORKDIR /app
COPY target/b05-wopi-host-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir /data && chown 10001:10001 /data
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/app.jar"]
