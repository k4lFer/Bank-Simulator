FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Layer 1: dependencies (cached unless pom.xml changes)
COPY shared-contracts/pom.xml ./shared-contracts/
COPY ledger-service/pom.xml ./ledger-service/
RUN mvn -f ledger-service/pom.xml dependency:go-offline -DskipTests 2>/dev/null || true

# Layer 2: shared-contracts (cached unless its source changes)
COPY shared-contracts/src ./shared-contracts/src
RUN mvn -f shared-contracts/pom.xml clean install -DskipTests -q

# Layer 3: service build (cached unless its source changes)
COPY ledger-service/src ./ledger-service/src
RUN mvn -f ledger-service/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:21-jre
COPY --from=build /app/ledger-service/target/*.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
