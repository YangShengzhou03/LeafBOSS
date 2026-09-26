FROM eclipse-temurin:17-alpine
WORKDIR /app
COPY leaf-boss-backend.jar /app/app.jar
EXPOSE 8081

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
