# Use an official Maven image as the build environment
FROM maven:3.8.4-openjdk-11 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the project source code
COPY pom.xml .
COPY src ./src

# Build the application with Maven
RUN mvn clean package -DskipTests

# Use an official OpenJDK runtime as the base image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the builder stage to the working directory
COPY --from=builder /app/target/turkish-verb-conjugator-1.0-SNAPSHOT.jar .

# Set the entrypoint command to run the application
CMD ["java", "-jar", "turkish-verb-conjugator-1.0-SNAPSHOT.jar"]
