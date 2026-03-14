# 基础镜像
FROM bellsoft/liberica-openjre-rocky:21
# FROM bellsoft/liberica-openjre-rocky:17
# FROM bellsoft/liberica-openjre-rocky:11
# FROM bellsoft/liberica-openjre-rocky:8
LABEL maintainer=jnpf-team

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 解决连接SQLServer安全错误
# 如您使用SQLServer数据库，把以下注释取消
# COPY security/java.security /opt/java/openjdk/lib/security

# 指定运行时的工作目录
WORKDIR /data/jnpfsoft/javaApi

# 将构建产物jar包拷贝到运行时目录中
COPY jnpf-admin/target/*.jar ./jnpf-admin.jar

# 指定容器内运行端口
EXPOSE 30000

# 指定容器启动时要运行的命令
ENTRYPOINT ["/bin/sh","-c","java -Dfile.encoding=utf8 -Djava.security.egd=file:/dev/./urandom -jar jnpf-admin.jar"]