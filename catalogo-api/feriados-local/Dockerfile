FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY src/ ./
RUN javac -d /out FeriadosLocalServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /out/ /app/
ENV FERIADOS_HOST=0.0.0.0
EXPOSE 8081
CMD ["java", "-cp", "/app", "FeriadosLocalServer"]
