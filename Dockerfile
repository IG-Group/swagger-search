FROM openjdk:8-jre-alpine

ENV SWAGGER_HOME=/config
WORKDIR /app
COPY target/swagger-search-*standalone.jar /app/swagger-search.jar

CMD ["java", "-jar", "swagger-search.jar"]