import urllib.request
import json

# 测试 RAG answer API
data = json.dumps({
    "query": "番茄叶片发黄怎么办",
    "mode": "knowledge",
    "top_k": 3
}).encode('utf-8')

req = urllib.request.Request(
    "http://localhost:9001/rag/answer",
    data=data,
    headers={"Content-Type": "application/json"},
    method="POST"
)

try:
    with urllib.request.urlopen(req, timeout=60) as resp:
        result = json.loads(resp.read())
        data = result.get("data", {})
        
        # 检查 references（后端返回的完整引用列表）
        refs = data.get("references", [])
        print(f"references count: {len(refs)}")
        if refs:
            print("\n=== First reference ===")
            print(json.dumps(refs[0], indent=2, ensure_ascii=False))
        
        # 检查 citedKnowledgeSources
        cks = data.get("citedKnowledgeSources", [])
        print(f"\ncitedKnowledgeSources count: {len(cks)}")
        
        # 检查 answerSummary
        print(f"\nanswerSummary: {data.get('answerSummary', 'N/A')[:200]}")
        
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()