FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY maven-settings.xml .
COPY src ./src
RUN mvn -s maven-settings.xml -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r app && useradd -r -g app -u 10001 app
COPY --chown=app:app --from=build /build/target/smart-agriculture-server-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
