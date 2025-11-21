FROM amazoncorretto:21-alpine

WORKDIR /app

# GitHub Actions에서 이미 빌드된 jar를 COPY
COPY build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]