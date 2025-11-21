FROM amazoncorretto:21-alpine

WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew bootJar && \
    cp build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
