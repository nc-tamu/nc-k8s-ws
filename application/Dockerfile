## Builder stage
# Here we compile the code
FROM maven:3.9.0-amazoncorretto-17 as builder

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app

RUN mvn -f /usr/src/app/pom.xml clean package

## Runtime stage
# Here we package the application into the runtime image
FROM openjdk:17-oracle as runtime
LABEL maintainer="Tarik Muhovic <tamu@netcompany.com>"
EXPOSE 8080
COPY --from=builder /usr/src/app/target/application.jar app.jar
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app.jar" \
]