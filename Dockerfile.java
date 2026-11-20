# Java 后端 Dockerfile
# 多阶段构建：Maven 构建 + JRE 运行

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 【新增 1】配置阿里云 Maven 镜像 + 全局重试参数
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <name>Aliyun Public Repository</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF

COPY pom.xml .

# 【新增 2】优化 go-offline 命令：增加重试次数 + 包含插件依赖
RUN mvn -B dependency:go-offline -q \
    -Daether.connector.http.retryHandler.count=5

COPY src ./src

# 【新增 3】打包时也加上重试参数，防止最后阶段偶发网络抖动
RUN mvn -B package -DskipTests -q \
    -Daether.connector.http.retryHandler.count=5

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]