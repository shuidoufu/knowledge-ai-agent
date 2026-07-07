FROM docker.m.daocloud.io/library/amazoncorretto:17-alpine
WORKDIR /app

COPY target/ai-agent-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8123

CMD ["java","--enable-preview" ,"-jar", "app.jar"]


# ================= 运行时打包 ==================
#FROM maven:3.9-amazoncorretto-21
#WORKDIR /app
#
#
#COPY pom.xml .
#COPY src ./src
#
#
#RUN mvn clean package -DskipTests
#
#
#EXPOSE 8123
#
#
#CMD ["java", "-jar", "/app/target/yu-ai-agent-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=prod"]