# Stage 1: Build the application with Maven
# We use an official image that has Java 17 and Maven pre-installed.
FROM maven:3.8-openjdk-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file first to leverage Docker's layer caching
COPY pom.xml .

# Copy the rest of your source code
COPY src ./src

# Run the Maven package command to build the executable .jar file.
# -DskipTests skips running tests, which is common for deployment builds.
RUN mvn -f /app/pom.xml clean package -DskipTests

# Stage 2: Create the final, smaller image for running the application
# We use a slim OpenJDK image which is much smaller and more secure.
FROM openjdk:17-slim

# Set the working directory
WORKDIR /app

# Copy the built .jar file from the 'build' stage into this final image
# Note: The name must match the artifactId and version in your pom.xml
COPY --from=build /app/target/debtstrategist-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application will run on (this should match your application.properties)
EXPOSE 8081

# The command to run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]