# Etapa 1: Compilar el proyecto con Maven dentro de la subcarpeta
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app
COPY . .
# Entramos a la carpeta ClinicaWeb para ejecutar Maven
RUN cd ClinicaWeb && mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación en Apache Tomcat
FROM tomcat:9.0-jdk11
RUN rm -rf /usr/local/tomcat/webapps/ROOT
# Copiamos el .war generado desde la subcarpeta
COPY --from=build /app/ClinicaWeb/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]