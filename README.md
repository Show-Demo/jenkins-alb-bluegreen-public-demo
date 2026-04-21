# order-api Jenkins Demo

这是一个按 Java 服务仓库结构组织的 Jenkins 发布仓库。

## 目录结构

```text
order-api/
  Jenkinsfile
  pom.xml
  Dockerfile
  k8s/
    dev/order-api.yaml
    test/order-api.yaml
    prod/order-api-bluegreen.yaml
```

## 分支和环境

- `dev` 分支：构建 dev 包，推送 `registry.example.com/dev/order-api:latest`，部署到 `dev` namespace。
- `test` 分支：构建 test 包，推送 `registry.example.com/test/order-api:latest`，部署到 `test` namespace。
- `master` 分支：构建 prod 包，推送 `registry.example.com/prod/order-api:latest`，先部署 green 版本，稳定后人工确认，再把 Ingress 从 blue 切到 green。

## Jenkins 任务

Jenkins 根据分支自动执行对应 stage：

- 推送 `dev` 分支，只执行 dev 相关步骤。
- 推送 `test` 分支，只执行 test 相关步骤。
- 推送 `master` 分支，只执行 prod 相关步骤。
