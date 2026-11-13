import http.client
import json

# 获取登录 token
conn = http.client.HTTPConnection("localhost", 8090, timeout=10)
headers = {"Content-Type": "application/json"}

# 登录
login_data = {"username": "admin", "password": "admin123"}
conn.request("POST", "/api/auth/login", json.dumps(login_data), headers)
resp = conn.getresponse()
login_result = json.loads(resp.read().decode())

print(f"登录状态: {resp.status}")
if login_result.get("data"):
    token = login_result["data"].get("token")
    print(f"Token 获取成功: {token[:30]}...")
else:
    print(f"登录失败: {login_result}")
    exit(1)

# 测试 RAG API
conn2 = http.client.HTTPConnection("localhost", 8090, timeout=120)
headers2 = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}

rag_data = {
    "query": "番茄叶片发黄怎么办",
    "mode": "knowledge",
    "top_k": 3
}

conn2.request("POST", "/api/ai/rag/chat", json.dumps(rag_data), headers2)
resp2 = conn2.getresponse()
result = json.loads(resp2.read().decode())

print(f"\nRAG API 状态: {resp2.status}")

if result.get("data"):
    data = result["data"]
    print("\n=== 所有顶级 keys ===")
    print(list(data.keys()))
    
    print("\n=== citedKnowledgeSources ===")
    cks = data.get("citedKnowledgeSources", [])
    print(f"数量: {len(cks)}")
    for i, ck in enumerate(cks[:3]):
        print(f"  [{i+1}] id={ck.get('id')}, score={ck.get('score')}, title={ck.get('title', '')[:40]}")
    
    print("\n=== citedKnowledgeGraph ===")
    ckg = data.get("citedKnowledgeGraph", [])
    print(f"数量: {len(ckg)}")
    if ckg:
        print(f"  [1] id={ckg[0].get('id')}, title={ckg[0].get('title')}")
    
    print("\n=== answerSummary (前200字) ===")
    print(data.get("answerSummary", "N/A")[:200])
else:
    print(f"API 失败: {result}")
