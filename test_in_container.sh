#!/bin/bash
# 在容器内执行：发送正确的 JSON 请求并检查响应

# 测试登录
echo "=== 测试登录 ==="
response=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
echo "响应: $response"
echo ""

# 解析 JSON（简单检查）
echo "=== 解析响应 ==="
echo "$response" | python3 -m json.tool 2>/dev/null || echo "JSON 解析失败"
