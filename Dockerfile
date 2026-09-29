# Etapa 1: Compilar el proyecto con Maven
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación en Apache Tomcat
FROM tomcat:9.0-jdk11
# Eliminar la aplicación por defecto de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/ROOT
# Copiar el archivo .war generado por Maven a la raíz del servidor Tomcat
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]