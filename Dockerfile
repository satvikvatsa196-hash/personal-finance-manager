# Build stage
FROM gradle:8.7.0-jdk21-jammy AS build
WORKDIR /app
COPY . .
# Build the application using native gradle command
RUN gradle build -x test

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
