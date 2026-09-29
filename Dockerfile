FROM tomcat:9.0-jdk11

# Eliminar la aplicación por defecto de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copiar tu archivo .war desde la carpeta dist hacia Tomcat
COPY dist/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]