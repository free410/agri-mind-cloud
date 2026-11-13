import urllib.request
import json
import sys

# 测试 RAG answer API（前端走的这个）
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
        print("Status:", resp.status)
        data = result.get("data", {})
        print("\n=== Response Structure ===")
        print("citedKnowledgeSources count:", len(data.get("citedKnowledgeSources", [])))
        print("references count:", len(data.get("references", [])))
        print("retrievalMetadata:", json.dumps(data.get("retrievalMetadata", {}), indent=2, ensure_ascii=False)[:500])

        # 打印 citedKnowledgeSources 详情
        cks = data.get("citedKnowledgeSources", [])
        print("\n=== citedKnowledgeSources ===")
        for i, ck in enumerate(cks[:3]):
            print(f"\n--- Item {i} ---")
            print(json.dumps(ck, indent=2, ensure_ascii=False))

except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)