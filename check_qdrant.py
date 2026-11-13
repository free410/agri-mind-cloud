import urllib.request
import json

try:
    req = urllib.request.Request("http://localhost:6333/collections", headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read())
        print("Collections:", json.dumps(data, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Error: {e}")
