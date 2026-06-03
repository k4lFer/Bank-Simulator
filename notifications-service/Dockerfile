FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Layer 1: dependencies (cached unless pom.xml changes)
COPY shared-contracts/pom.xml ./shared-contracts/
COPY notifications-service/pom.xml ./notifications-service/
RUN mvn -f notifications-service/pom.xml dependency:go-offline -DskipTests 2>/dev/null || true

# Layer 2: shared-contracts (cached unless its source changes)
COPY shared-contracts/src ./shared-contracts/src
RUN mvn -f shared-contracts/pom.xml clean install -DskipTests -q

# Layer 3: service build (cached unless its source changes)
COPY notifications-service/src ./notifications-service/src
RUN mvn -f notifications-service/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:21-jre
COPY --from=build /app/notifications-service/target/*.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
