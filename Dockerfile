# NOTE:
# If you cannot access Docker Hub (auth.docker.io / registry-1.docker.io) due to network restrictions,
# switch to a Docker Hub proxy by setting image names like:
#   docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17
#   docker.m.daocloud.io/library/eclipse-temurin:17-jre
#
# Default values below use a proxy to avoid auth.docker.io timeouts in restricted networks.
FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8189
ENTRYPOINT ["java", "-jar", "app.jar"]