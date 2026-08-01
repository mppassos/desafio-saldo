# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os POMs primeiro para aproveitar o cache de camadas do Docker
COPY pom.xml .
COPY db-migrations/pom.xml db-migrations/pom.xml
COPY db-migrations/src db-migrations/src
COPY api/pom.xml api/pom.xml
COPY ingestor/pom.xml ingestor/pom.xml

# Baixa as dependências antes de copiar o código-fonte (cache)
RUN mvn -B -pl api -am dependency:go-offline -q || true

# Copia o código-fonte e compila
COPY api/src api/src
COPY ingestor/src ingestor/src
RUN mvn -B -pl api -am package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/api/target/*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
