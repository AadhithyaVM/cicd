# Runtime image. The JAR is built and tested by Maven in the Jenkins pipeline first,
# so the exact artifact that passed the tests is what gets containerised.
FROM eclipse-temurin:17-jre-alpine

ARG BUILD_NUMBER=local
ENV APP_VERSION=${BUILD_NUMBER} \
    PORT=8080

# Run as a non-root user (security best practice)
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/app.jar /app/app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
