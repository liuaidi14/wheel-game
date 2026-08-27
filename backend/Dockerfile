# 使用 Eclipse Temurin (AdoptOpenJDK) 的轻量级 JDK 17 镜像，国内加速器支持更好
FROM eclipse-temurin:17-jre-alpine

# 设置时区
ENV TZ=Asia/Shanghai

# 工作目录
WORKDIR /app

# 将 target 下打包好的 jar 复制进来（注意文件名，如果生成了不一样的名字请相应修改）
COPY target/wheelGame-0.0.1-SNAPSHOT.jar app.jar

# 暴露 8080 端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]