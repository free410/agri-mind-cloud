import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context

BASE = "http://localhost:8080/api"

def login():
    data = json.dumps({"username": "admin", "password": "admin123"}).encode('utf-8')
    req = urllib.request.Request(
        f"{BASE}/auth/login",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = resp.read().decode('utf-8')
            print(f"Status: {resp.status}")
            result = json.loads(body)
            print(f"Code: {result.get('code')}")
            print(f"Message: {result.get('message')}")
            if result.get('code') == 200:
                token = result.get('data', {}).get('token')
                print(f"Token: {token[:40]}...")
                return token
            else:
                print(f"Error: {body}")
                return None
    except Exception as e:
        print(f"Login failed: {e}")
        import traceback
        traceback.print_exc()
        return None

def test_rag(token):
    data = json.dumps({
        "query": "番茄叶片发黄怎么办",
        "mode": "knowledge",
        "top_k": 3
    }).encode('utf-8')
    req = urllib.request.Request(
        f"{BASE}/ai/rag/chat",
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            body = resp.read().decode('utf-8')
            result = json.loads(body)
            print(f"\nRAG Status: {resp.status}")
            print(f"Code: {result.get('code')}")
            data = result.get('data', {})
            print(f"Response fields: {list(data.keys())}")
            cks = data.get('citedKnowledgeSources', [])
            print(f"citedKnowledgeSources length: {len(cks)}")
            if cks:
                print(f"First: id={cks[0].get('id')}, score={cks[0].get('score')}")
            return len(cks) > 0
    except Exception as e:
        print(f"RAG failed: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("=== 登录测试 ===")
    token = login()
    if token:
        print("\n=== RAG 测试 ===")
        success = test_rag(token)
        if success:
            print("\n✅ 测试成功！citedKnowledgeSources 有数据")
        else:
            print("\n❌ 测试失败：citedKnowledgeSources 为空")
    else:
        print("登录失败，无法测试 RAG")
