FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime

FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxxf86vm1 \
    xauth \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/client-javafx-1.0-SNAPSHOT.jar ./
COPY --from=build /app/target/lib ./lib/

ENTRYPOINT ["java", \
    "--module-path", "lib", \
    "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base", \
    "-cp", "client-javafx-1.0-SNAPSHOT.jar", \
    "com.unibusiness.client.Main"]
