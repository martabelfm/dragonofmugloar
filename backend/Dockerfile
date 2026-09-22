FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -U -DskipTests package

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 mugloar
WORKDIR /app
COPY --from=build /workspace/target/dragon-trainer-api-*.jar app.jar
USER mugloar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
