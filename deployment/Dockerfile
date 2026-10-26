FROM eclipse-temurin:17-jre
RUN groupadd --system appuser && useradd --system --gid appuser appuser
WORKDIR /app
COPY UserAdministration.jar app.jar
# Run with a non-root user to avoid running the container with excessive privileges
USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
