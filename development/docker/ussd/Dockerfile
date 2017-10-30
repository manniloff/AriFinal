FROM tomcat


RUN apt-get update
RUN apt-get install -y --no-install-recommends lksctp-tools


ADD docker/ussd/conf /usr/local/tomcat/conf
ADD docker/ussd/lib /usr/local/tomcat/lib
ADD SigtranConector/target/SigtranConnector.jar /usr/local/tomcat/lib
ADD SigtranConector/target/lib /usr/local/tomcat/lib
ADD SigtranManager/target/SigtranManager.war /usr/local/tomcat/webapps/
ADD UssdGate/target/UssdGate.war /usr/local/tomcat/webapps/

EXPOSE 7081
EXPOSE 2905

CMD ["catalina.sh", "run"]
