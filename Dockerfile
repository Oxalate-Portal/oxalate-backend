FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080

RUN adduser -D oxalate
USER oxalate

ADD service/target/oxalate-service-*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
