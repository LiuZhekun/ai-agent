FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn -pl ai-agent-demo -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/ai-agent-demo/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
