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
# O plano gratis do Render tem 512 MB de RAM no total. Heap fixo em 256 MB e
# metaspace/code cache limitados para o processo inteiro caber; menos threads
# do Tomcat e JIT so no nivel 1 (sobe mais rapido na CPU fraca do plano).
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENV SERVER_TOMCAT_THREADS_MAX=20
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
