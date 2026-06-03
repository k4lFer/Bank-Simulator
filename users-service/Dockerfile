FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Layer 1: dependencies (cached unless pom.xml changes)
COPY shared-contracts/pom.xml ./shared-contracts/
COPY users-service/pom.xml ./users-service/
RUN mvn -f users-service/pom.xml dependency:go-offline -DskipTests 2>/dev/null || true

# Layer 2: shared-contracts (cached unless its source changes)
COPY shared-contracts/src ./shared-contracts/src
RUN mvn -f shared-contracts/pom.xml clean install -DskipTests -q

# Layer 3: service build (cached unless its source changes)
COPY users-service/src ./users-service/src
RUN mvn -f users-service/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:21-jre
COPY --from=build /app/users-service/target/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
