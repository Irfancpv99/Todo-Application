FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libfreetype6 \
    libxfixes3

WORKDIR /app

COPY target/todo-application-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

ENV DB_URL=jdbc:postgresql://db:5432/todo_db \
    DB_USER=postgres \
    DB_PASSWORD=postgres

CMD ["java", "-jar", "app.jar"]