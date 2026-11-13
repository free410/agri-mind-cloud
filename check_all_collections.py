import urllib.request
import json

# 检查所有集合
req = urllib.request.Request("http://localhost:6333/collections", headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(req, timeout=5) as resp:
        data = json.loads(resp.read())
        print("All collections:")
        for col in data.get("result", {}).get("collections", []):
            name = col.get("name", "")
            # 获取每个集合的点数
            req2 = urllib.request.Request(f"http://localhost:6333/collections/{name}", headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req2, timeout=5) as resp2:
                colinfo = json.loads(resp2.read())
                points = colinfo.get("result", {}).get("points_count", 0)
                print(f"  - {name}: {points} points")
except Exception as e:
    print(f"Error: {e}")
