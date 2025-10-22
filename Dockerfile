FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN --mount=type=secret,id=gpr.user \
    --mount=type=secret,id=gpr.key \
    mkdir -p /root/.gradle && \
    echo "gpr.user=$(cat /run/secrets/gpr.user)" >> /root/.gradle/gradle.properties && \
    echo "gpr.key=$(cat /run/secrets/gpr.key)"  >> /root/.gradle/gradle.properties && \
    ./gradlew --no-daemon clean bootJar && \
    rm -f /root/.gradle/gradle.properties

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
