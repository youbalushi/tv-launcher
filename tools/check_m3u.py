"""Checks every stream in an M3U playlist and writes the working ones to a JSON file."""
import json, re, sys, urllib.request, ssl
from concurrent.futures import ThreadPoolExecutor

src, out = sys.argv[1], sys.argv[2]
text = urllib.request.urlopen(src, timeout=60).read().decode("utf-8", "replace")
entries, cur = [], None
for line in text.splitlines():
    line = line.strip()
    if line.startswith("#EXTINF"):
        name = line.split(",", 1)[-1].strip()
        attrs = dict(re.findall(r'([\w-]+)="([^"]*)"', line))
        cur = {"name": name, "group": attrs.get("group-title", ""), "tvg": attrs.get("tvg-id", ""), "logo": attrs.get("tvg-logo", ""), "headers": {}}
    elif line.startswith("#EXTVLCOPT") and cur is not None:
        if "http-referrer=" in line:
            cur["headers"]["Referer"] = line.split("=", 1)[1]
        if "http-user-agent=" in line:
            cur["headers"]["User-Agent"] = line.split("=", 1)[1]
    elif line and not line.startswith("#") and cur is not None:
        cur["url"] = line
        entries.append(cur)
        cur = None

ctx = ssl.create_default_context()
def check(e):
    h = {"User-Agent": "Mozilla/5.0 (Linux; Android TV)"}
    h.update(e["headers"])
    try:
        req = urllib.request.Request(e["url"], headers=h)
        with urllib.request.urlopen(req, timeout=12, context=ctx) as r:
            body = r.read(300).decode("utf-8", "replace")
            e["ok"] = r.status == 200 and "#EXTM3U" in body
    except Exception as ex:
        e["ok"] = False
        e["err"] = str(ex)[:80]
    return e

with ThreadPoolExecutor(32) as pool:
    results = list(pool.map(check, entries))
ok = [r for r in results if r["ok"]]
json.dump({"total": len(results), "working": len(ok), "channels": results}, open(out, "w"), ensure_ascii=False, indent=1)
print(f"::notice::total={len(results)} working={len(ok)}")
