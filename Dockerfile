FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw && ./mvnw -B -q verify

FROM eclipse-temurin:21-jre-alpine
ENV SPRING_PROFILES_ACTIVE=prod
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /src/target/appointment-management-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
