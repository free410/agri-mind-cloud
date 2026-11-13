import urllib.request
import json

# 测试 RAG answer API（前端用的就是这个）
data = json.dumps({
    "query": "番茄叶片发黄怎么办",
    "mode": "knowledge",
    "top_k": 3
}).encode('utf-8')

req = urllib.request.Request(
    "http://localhost:9001/rag/answer",
    data=data,
    headers={
        "Content-Type": "application/json"
    },
    method="POST"
)

try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        result = json.loads(resp.read())
        print("Status:", resp.status)
        data = result.get("data", {})
        print("\nResponse keys:", list(data.keys()))
        print("\ncitedKnowledgeSources count:", len(data.get("citedKnowledgeSources", [])))
        print("references count:", len(data.get("references", [])))
        print("retrievalMetadata:", json.dumps(data.get("retrievalMetadata", {}), indent=2, ensure_ascii=False))

        # 打印 citedKnowledgeSources 详情
        cks = data.get("citedKnowledgeSources", [])
        if cks:
            print("\nFirst citedKnowledgeSource:")
            print(json.dumps(cks[0], indent=2, ensure_ascii=False)[:600])
except Exception as e:
    print(f"Error: {e}")
