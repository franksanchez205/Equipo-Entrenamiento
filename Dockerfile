# Etapa 1: Compilar la aplicación (Cambiamos a una imagen de Maven con Java actual)
FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Crear el contenedor final de ejecución (Cambiamos openjdk por eclipse-temurin)
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]