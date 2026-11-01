# Build the Spring Boot application using Java 17.
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Gradle configuration and wrapper.
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Copy application source code.
COPY src ./src

# Allow the Gradle wrapper to execute.
RUN chmod +x ./gradlew

# Build the executable Spring Boot JAR.
RUN ./gradlew bootJar --no-daemon

# Run the application using the Java 17 runtime.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the compiled application from the build stage.
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Start Spring Boot.
ENTRYPOINT ["java", "-jar", "app.jar"]