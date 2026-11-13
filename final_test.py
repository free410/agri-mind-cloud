import urllib.request
import json
import time

BASE = "http://localhost:8090/api"

def login():
    """登录获取 token"""
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
            result = json.loads(body)
            if result.get('code') == 200:
                token = result.get('data', {}).get('token')
                print(f"✅ 登录成功")
                return token
            else:
                print(f"❌ 登录失败: {result}")
                return None
    except Exception as e:
        print(f"❌ 登录异常: {e}")
        return None

def test_rag(token):
    """测试 RAG API"""
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
            data = result.get('data', {})
            
            print(f"\n=== RAG 响应分析 ===")
            print(f"HTTP状态: {resp.status}")
            print(f"业务code: {result.get('code')}")
            print(f"响应字段: {list(data.keys())}")
            
            cks = data.get('citedKnowledgeSources', [])
            print(f"citedKnowledgeSources 数量: {len(cks)}")
            
            if cks:
                print(f"\n✅ 第一条知识来源:")
                first = cks[0]
                print(f"  id: {first.get('id')}")
                print(f"  title: {first.get('title', '')[:60]}")
                print(f"  score: {first.get('score')}")
                print(f"  content长度: {len(first.get('content', ''))}")
                return True
            else:
                print(f"\n❌ citedKnowledgeSources 为空数组")
                print(f"answerSummary: {data.get('answerSummary', '')[:200]}")
                return False
    except Exception as e:
        print(f"❌ RAG请求异常: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("=== 智慧农业 RAG 系统测试 ===\n")
    token = login()
    if token:
        success = test_rag(token)
        if success:
            print("\n✅✅✅ 测试通过！前端将正常显示知识来源")
        else:
            print("\n⚠️  RAG API 正常，但知识来源为空，可能需要检查向量数据库")
    else:
        print("\n��� 登录失败，请检查后端服务")
