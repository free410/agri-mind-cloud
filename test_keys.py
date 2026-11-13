import urllib.request
import json

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
        
        print("=== All top-level keys ===")
        print(list(data.keys()))
        
        print("\n=== cited_knowledge_sources ===")
        cks = data.get("cited_knowledge_sources", [])
        print(f"count: {len(cks)}")
        for i, ck in enumerate(cks[:2]):
            print(f"  Item {i}:", json.dumps(ck, indent=2, ensure_ascii=False)[:300])
        
        print("\n=== citedKnowledgeSources (camelCase) ===")
        cks2 = data.get("citedKnowledgeSources", [])
        print(f"count: {len(cks2)}")
        for i, ck in enumerate(cks2[:2]):
            print(f"  Item {i}:", json.dumps(ck, indent=2, ensure_ascii=False)[:300])
        
        print("\n=== references ===")
        refs = data.get("references", [])
        print(f"count: {len(refs)}")
        if refs:
            print(f"  First: id={refs[0].get('id')}, score={refs[0].get('score')}, title={refs[0].get('title')}")
        
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()