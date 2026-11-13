import urllib.request
import json

BASE_URL = "http://localhost:8090/api"

def login():
    """登录获取 token"""
    data = json.dumps({"username": "admin", "password": "admin123"}).encode('utf-8')
    req = urllib.request.Request(
        f"{BASE_URL}/auth/login",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            result = json.loads(resp.read())
            token = result.get("data", {}).get("token")
            print(f"✅ 登录成功")
            return token
    except Exception as e:
        print(f"❌ 登录失败: {e}")
        return None

def test_rag_api(token):
    """测试 RAG API 响应结构"""
    data = json.dumps({
        "query": "番茄叶片发黄怎么办",
        "mode": "knowledge",
        "top_k": 3
    }).encode('utf-8')
    
    req = urllib.request.Request(
        f"{BASE_URL}/ai/rag/chat",
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        },
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read())
            data = result.get("data", {})
            
            print("\n=== 响应字段检查 ===")
            print(f"所有顶级 keys: {list(data.keys())}")
            
            print("\n=== citedKnowledgeSources (前端使用字段) ===")
            cks = data.get("citedKnowledgeSources", [])
            print(f"  类型: {type(cks).__name__}")
            print(f"  长度: {len(cks)}")
            if cks:
                print(f"  第一条: id={cks[0].get('id')}")
                print(f"          title={cks[0].get('title', '')[:50]}")
                print(f"          score={cks[0].get('score')}")
                print(f"          content长度: {len(cks[0].get('content', ''))}")
            else:
                print("  ⚠️ 数组为空")
            
            print("\n=== answerSummary ===")
            summary = data.get("answerSummary", "")
            print(f"  长度: {len(summary)}")
            print(f"  前200字: {summary[:200]}")
            
            return len(cks) > 0
    except Exception as e:
        print(f"❌ 请求失败: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    token = login()
    if token:
        success = test_rag_api(token)
        if success:
            print("\n✅ 测试通过：citedKnowledgeSources 字段正常显示")
        else:
            print("\n❌ 测试失败：citedKnowledgeSources 字段为空")
    else:
        print("无法获取 token，终止测试")
