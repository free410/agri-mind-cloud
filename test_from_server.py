import urllib.request
import json

# 从 smartagri-server 容器内部访问 RAG 服务
data = json.dumps({
    "query": "番茄叶片发黄",
    "mode": "knowledge",
    "top_k": 3
}).encode('utf-8')

req = urllib.request.Request(
    "http://smartagri-rag:9001/rag/answer",
    data=data,
    headers={"Content-Type": "application/json"},
    method="POST"
)

try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        result = json.loads(resp.read())
        data = result.get("data", {})
        print("citedKnowledgeSources count:", len(data.get("citedKnowledgeSources", [])))
        print("references count:", len(data.get("references", [])))
        print("\nFull response:")
        print(json.dumps(data, indent=2, ensure_ascii=False)[:2000])
except Exception as e:
    print(f"Error: {e}")
