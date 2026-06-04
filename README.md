# thymeleaf-test

一个用于**在线预览渲染结果**的模板测试小项目，支持：

- **Thymeleaf**（Spring Boot `spring-boot-starter-thymeleaf`）
- **Velocity**（`velocity-engine-core`，在 Playground 页面里直接渲染字符串模板）

默认启动后访问：`http://localhost:8189/playground`

## 环境要求

- **JDK 17+**（项目基于 Spring Boot 3.2.0）
- **Maven 3.9+**（本地运行时）
- **Docker**（如果用 `start.sh` / 容器方式运行）

## 本地运行（推荐开发调试）

在项目根目录执行：

```bash
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn clean package -DskipTests
java -jar target/thymeleaf-test-1.0.0.jar
```

启动后打开：

- `http://localhost:8189/playground`：模板渲染 Playground（支持 Thymeleaf / Velocity）
- `http://localhost:8189/test-email`：渲染内置示例邮件模板（Thymeleaf）

## Docker 运行（一键）

项目自带 `start.sh`，会自动：

- 清理占用 **8189** 的容器端口映射（如果有）
- `docker build` 构建镜像
- `docker run -d -p 8189:8189` 后台启动容器

运行：

```bash
./start.sh
```

打开：`http://localhost:8189/playground`

查看日志：

```bash
docker logs -f thymeleaf-test
```

停止并删除容器：

```bash
docker rm -f thymeleaf-test
```

## 接口说明

### 页面

- **`GET /`**：重定向到 `/playground`
- **`GET /playground`**：Playground 页面（在页面中选择引擎、粘贴模板与 JSON 数据并预览）
- **`GET /test-email`**：渲染 `src/main/resources/templates/email-template.html` 示例（后端写死了一份示例数据）

### 渲染 API（Playground 使用）

- **`POST /playground/render`**
  - **Content-Type**: `application/json`
  - **Body**:
    - `engine`: `"thymeleaf"` 或 `"velocity"`（不传默认 thymeleaf）
    - `template`: 模板字符串（必填）
    - `modelJson`: JSON 对象字符串（可选；会解析成 Map 传给模板）

示例：

```bash
curl -sS -X POST "http://localhost:8189/playground/render" \
  -H "Content-Type: application/json" \
  -d '{
    "engine": "thymeleaf",
    "template": "<p th:text=\"${name}\">name</p>",
    "modelJson": "{\"name\":\"张三\"}"
  }'
```

成功返回：

- `renderedHtml`: 渲染后的 HTML

失败返回（HTTP 400）：

- `error`: 错误信息（尽量包含行号/列号提示）

## 目录结构（关键文件）

- `src/main/java/com/test/EmailTestApplication.java`：Spring Boot 入口 + `/test-email`
- `src/main/java/com/test/PlaygroundController.java`：`/playground` 与 `/playground/render`
- `src/main/resources/templates/playground.html`：Playground 前端页面
- `src/main/resources/templates/email-template.html`：示例邮件模板（Thymeleaf）
- `Dockerfile`：多阶段构建（Maven 构建 + JRE 运行）
- `start.sh`：一键 Docker 构建并运行

## 常见问题

### 1) 8189 端口被占用

- 改用别的端口启动（本地运行）：`--server.port=xxxx`（默认端口已在 `src/main/resources/application.properties` 里设置为 8189）
- 或停止占用 8189 的进程/容器后再启动

### 2) Docker 拉镜像很慢 / 访问不到 Docker Hub

`Dockerfile` 默认使用了镜像代理（`docker.m.daocloud.io`）来避免 `auth.docker.io` 超时。

如果你的网络环境可以直连 Docker Hub，也可以把 `Dockerfile` 里的基础镜像改回：

- `maven:3.9-eclipse-temurin-17`
- `eclipse-temurin:17-jre`


