FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/rpc-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
