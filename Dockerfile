FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/pmmt.jar /pmmt/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/pmmt/app.jar"]
