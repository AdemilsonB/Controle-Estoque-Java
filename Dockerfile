FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM tomcat:10.1-jre17
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/controle-estoque.war /usr/local/tomcat/webapps/controle-estoque.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
