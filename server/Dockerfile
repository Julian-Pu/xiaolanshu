# 小蓝书后端镜像（生产参考）
# 构建：server 目录下先执行 .\mvnw.cmd package，再 docker build -t xiaolanshu-server .
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/xiaolanshu-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
