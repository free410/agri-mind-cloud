import urllib.request
import json

# 通过 Spring Boot API 测试（使用正确登录获取 token）
# 先获取 token
login_data = json.dumps({
    "username": "admin",
    "password": "admin123"
}).encode('utf-8')

login_req = urllib.request.Request(
    "http://localhost:8090/api/auth/login",
    data=login_data,
    headers={"Content-Type": "application/json"},
    method="POST"
)

try:
    with urllib.request.urlopen(login_req, timeout=10) as resp:
        login_result = json.loads(resp.read())
        print(f"登录响应: {json.dumps(login_result, indent=2, ensure_ascii=False)}")
        token = login_result.get("data", {}).get("token") or login_result.get("token")
        print(f"获取 token: {token[:20] if token else 'None'}...")
except Exception as e:
    print(f"登录失败: {e}")
    token = None

if not token:
    print("无法获取 token，跳过测试")
    exit(1)

# 测试 RAG API
rag_data = json.dumps({
    "query": "番茄叶片发黄怎么办",
    "mode": "knowledge",
    "top_k": 3
}).encode('utf-8')

rag_req = urllib.request.Request(
    "http://localhost:8090/api/ai/rag/chat",
    data=rag_data,
    headers={
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    },
    method="POST"
)

try:
    with urllib.request.urlopen(rag_req, timeout=120) as resp:
        result = json.loads(resp.read())
        data = result.get("data", {})
        
        print("\n=== 所有顶级 keys ===")
        print(list(data.keys()))
        
        print("\n=== citedKnowledgeSources (camelCase) ===")
        cks = data.get("citedKnowledgeSources", [])
        print(f"count: {len(cks)}")
        for i, ck in enumerate(cks[:2]):
            print(f"  Item {i}:", json.dumps(ck, indent=2, ensure_ascii=False)[:500])
        
        print("\n=== citedKnowledgeGraph ===")
        ckg = data.get("citedKnowledgeGraph", [])
        print(f"count: {len(ckg)}")
        if ckg:
            print(f"  First item: {json.dumps(ckg[0], indent=2, ensure_ascii=False)[:300]}")
        
        print("\n=== answerSummary ===")
        print(data.get("answerSummary", "N/A")[:200])
        
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
