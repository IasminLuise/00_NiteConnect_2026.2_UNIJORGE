FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --chown=10001:10001 target/nite-connect-api-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--nite.bootstrap.enabled=false"]
