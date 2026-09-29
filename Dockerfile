# Etapa 1: Compilar el proyecto usando Apache Ant
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app

# Instalamos Ant en el contenedor
RUN apt-get update && apt-get install -y ant

# Copiamos todo el proyecto al contenedor
COPY . .

# Ejecutamos la compilación con Ant (genera el archivo .war en la carpeta dist)
RUN ant clean dist

# Etapa 2: Ejecutar la aplicación en Apache Tomcat
FROM tomcat:9.0-jdk11
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copiamos el archivo .war generado por Ant hacia Tomcat
COPY --from=build /app/dist/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]