# Imagem do back-end (API Spring Boot) usada no deploy do Render.
# Etapa 1: compila o JAR com a imagem oficial do Maven.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: so o JRE e o JAR, para a imagem final ficar enxuta.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# O plano gratis do Render tem 512 MB de RAM: limita o heap para caber.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
