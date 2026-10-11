FROM tomcat:10.1-jdk17

# Hora de Perú para Java (fechas de bloqueo, último acceso y reportes diarios)
ENV JAVA_OPTS="-Duser.timezone=America/Lima"

# Limpiar el directorio ROOT por defecto de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copiar tu aplicación manteniendo el nombre ClinicaWeb.war
COPY dist/*.war /usr/local/tomcat/webapps/ClinicaWeb.war

RUN mkdir -p /usr/local/tomcat/webapps/ROOT && \
    echo '<!DOCTYPE html><html><head><meta http-equiv="refresh" content="0; url=/ClinicaWeb/"></head><body></body></html>' > /usr/local/tomcat/webapps/ROOT/index.html
