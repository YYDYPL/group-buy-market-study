# 在 app 模块目录执行；要求 target/group-buy-market-app.jar 已由 Maven 打包生成。
# 普通镜像构建使用当前 Docker 主机架构，标签 3.0 会覆盖本地同名标签。
docker build -t fuzhengwei/group-buy-market-app:3.0 -f ./Dockerfile .

# 多架构构建示例：需要提前创建 buildx builder；--push 会直接发布到镜像仓库。
# docker buildx build --load --platform linux/amd64,linux/arm64 -t fuzhengwei/group-buy-market-app:1.2 -f ./Dockerfile . --push
