FROM tomcat:9.0-jdk11

# Copiar el WAR manteniendo el nombre ClinicaWeb para respetar las rutas originales
COPY dist/*.war /usr/local/tomcat/webapps/ClinicaWeb.war

EXPOSE 8080
CMD ["catalina.sh", "run"]