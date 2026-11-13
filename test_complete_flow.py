import http.client
import json
import sys

def test_api_flow():
    """测试完整的 API 调用流程"""
    
    # Step 1: 获取 token
    print("=== Step 1: 登录获取 Token ===")
    conn = http.client.HTTPConnection("localhost", 8090, timeout=10)
    login_data = json.dumps({"username": "admin", "password": "admin123"})
    headers = {"Content-Type": "application/json"}
    
    try:
        conn.request("POST", "/api/auth/login", login_data, headers)
        resp = conn.getresponse()
        body = resp.read().decode()
        print(f"登录状态: {resp.status}")
        print(f"响应: {body}")
        
        if resp.status != 200:
            print("登录失败")
            return
        
        login_result = json.loads(body)
        token = login_result.get("data", {}).get("token")
        if not token:
            print("未获取到 token")
            return
        print(f"Token 获取成功: {token[:30]}...")
    except Exception as e:
        print(f"登录异常: {e}")
        return
    finally:
        conn.close()
    
    # Step 2: 测试 RAG API
    print("\n=== Step 2: 测试 RAG API ===")
    conn2 = http.client.HTTPConnection("localhost", 8090, timeout=120)
    rag_data = json.dumps({
        "query": "番茄叶片发黄怎么办",
        "mode": "knowledge",
        "top_k": 3
    })
    headers2 = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }
    
    try:
        conn2.request("POST", "/api/ai/rag/chat", rag_data, headers2)
        resp2 = conn2.getresponse()
        body2 = resp2.read().decode()
        print(f"RAG API 状态: {resp2.status}")
        print(f"响应预览: {body2[:2000]}")
        
        if resp2.status == 200:
            result = json.loads(body2)
            data = result.get("data", {})
            print("\n=== 响应结构分析 ===")
            print(f"顶级 keys: {list(data.keys())}")
            print(f"citedKnowledgeSources 是否存在: {'citedKnowledgeSources' in data}")
            cks = data.get("citedKnowledgeSources", [])
            print(f"citedKnowledgeSources 数量: {len(cks)}")
            if cks:
                print("第一条知识来源:")
                print(f"  id: {cks[0].get('id')}")
                print(f"  title: {cks[0].get('title')}")
                print(f"  score: {cks[0].get('score')}")
                print(f"  content: {cks[0].get('content', '')[:100]}")
            else:
                print("citedKnowledgeSources 为空数组")
        else:
            print(f"请求失败: {body2}")
    except Exception as e:
        print(f"RAG API 异常: {e}")
        import traceback
        traceback.print_exc()
    finally:
        conn2.close()

if __name__ == "__main__":
    test_api_flow()
