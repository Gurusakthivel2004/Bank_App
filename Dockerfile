FROM tomcat:9.0-jdk8

WORKDIR /usr/local/tomcat/webapps/

COPY dist/Bank_Application.war .

EXPOSE 8080

CMD ["catalina.sh", "run"]