#!/usr/bin/env bash
set -euo pipefail

echo "正在启动模板渲染测试服务器（Thymeleaf / Velocity）..."

# Make sure we don't accidentally run an old container/image when build fails,
# and avoid binding conflicts on port 8080.
if ids="$(docker ps -q --filter "publish=8080")" && [[ -n "${ids}" ]]; then
  echo "发现占用 8080 的容器，正在清理..."
  docker rm -f ${ids}
fi
docker rm -f thymeleaf-test 2>/dev/null || true

docker build -t thymeleaf-test .
docker run -d --name thymeleaf-test -p 8080:8080 thymeleaf-test >/dev/null
echo "已启动: http://localhost:8080/playground"
echo "查看日志: docker logs -f thymeleaf-test"