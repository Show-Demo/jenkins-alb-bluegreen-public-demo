FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/order-api.jar /app/order-api.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/order-api.jar"]
