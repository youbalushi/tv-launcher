"""Deep check of an M3U file: master playlist -> first variant -> last segment must all download.
Prints one GitHub annotation per channel."""
import re, sys, urllib.request, urllib.parse
from concurrent.futures import ThreadPoolExecutor

UA = "Mozilla/5.0 (Linux; Android 11; MiTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
src = sys.argv[1]
if src.startswith("http"):
    text = urllib.request.urlopen(urllib.request.Request(src, headers={"User-Agent": UA}), timeout=30).read().decode("utf-8", "replace")
    open("fetched.m3u", "w", encoding="utf-8").write(text)
else:
    text = open(src, encoding="utf-8").read()
entries, cur = [], None
for line in text.splitlines():
    line = line.strip()
    if line.startswith("#EXTINF"):
        cur = {"name": line.split(",", 1)[-1].strip()[:40]}
    elif line and not line.startswith("#") and cur is not None:
        cur["url"] = line; entries.append(cur); cur = None

def get(url, n=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=15) as r:
        return r.geturl(), (r.read(n) if n else r.read())

def lines(b): return [l.strip() for l in b.decode("utf-8", "replace").splitlines() if l.strip() and not l.startswith("#")]

def check(e):
    try:
        u, body = get(e["url"])
        if b"#EXTM3U" not in body: return e["name"], "not a playlist"
        items = lines(body)
        if b"#EXT-X-STREAM-INF" in body:
            u, body = get(urllib.parse.urljoin(u, items[-1]))  # lowest listed variant
            items = lines(body)
        seg = urllib.parse.urljoin(u, items[-1])
        _, data = get(seg, 200000)
        return e["name"], f"OK ({len(data)//1024} KB segment)"
    except Exception as ex:
        return e["name"], "FAIL " + str(ex)[:90]

results = []
print(f"::notice::entries: {len(entries)}")
with ThreadPoolExecutor(16) as p:
    for i, (name, res) in enumerate(p.map(check, entries)):
        results.append(f"{res} :: {name} :: {entries[i]['url']}")

# All results in one annotation (GitHub keeps only 10 per kind).
for k in range(0, len(results), 25):
    print("::error::" + " || ".join(results[k:k + 25]))
