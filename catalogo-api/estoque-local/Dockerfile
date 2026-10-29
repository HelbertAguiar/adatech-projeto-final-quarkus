# Aula 04: mesmo servidor HTTP do JDK, executado em rede Docker isolada.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY src/ ./
RUN javac -d /out EstoqueLocalServer.java EstoqueHealthCheck.java \
    && cp products.json /out/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /out/ /app/
EXPOSE 8081
# Expor na rede Docker (sem publicar a porta no host). Fora do Docker, o default continua 127.0.0.1.
CMD ["java", "-cp", "/app", "EstoqueLocalServer", "--host", "0.0.0.0"]
