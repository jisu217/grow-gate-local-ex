FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY backend/BE-02/gradlew backend/BE-02/settings.gradle backend/BE-02/build.gradle ./
COPY backend/BE-02/gradle ./gradle
RUN chmod +x ./gradlew

COPY backend/BE-02/src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/build/libs/*.jar /app/growgate-be.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/growgate-be.jar"]
