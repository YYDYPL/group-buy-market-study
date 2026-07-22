# Java 8 精简运行时；版本应与父 POM 中编译目标 1.8 保持一致。
FROM openjdk:8-jre-slim

# 镜像维护者信息（MAINTAINER 为旧版 Dockerfile 指令，仅作为元数据保留）。
MAINTAINER xiaofuge

# 业务启动参数占位，可在 docker run 时通过环境变量传入，例如切换 Spring profile。
ENV PARAMS=""

# 将容器时区设为中国标准时间，保证日志时间与业务时间一致。
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Maven package 生成固定名称的可执行 JAR，并复制到容器根目录。
ADD target/group-buy-market-app.jar /group-buy-market-app.jar

# 使用 shell 入口以展开 JAVA_OPTS 和 PARAMS：前者用于 JVM 参数，后者用于 Spring Boot 参数。
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /group-buy-market-app.jar $PARAMS"]
