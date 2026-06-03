FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Layer 1: dependencies (cached unless pom.xml changes)
COPY api-gateway/pom.xml ./api-gateway/
RUN mvn -f api-gateway/pom.xml dependency:go-offline -DskipTests 2>/dev/null || true

# Layer 2: service build (cached unless source changes)
COPY api-gateway/src ./api-gateway/src
RUN mvn -f api-gateway/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:21-jre
COPY --from=build /app/api-gateway/target/*.jar /app/app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
