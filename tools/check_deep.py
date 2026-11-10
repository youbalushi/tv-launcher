"""Deep check of an M3U file: master playlist -> first variant -> last segment must all download.
Prints one GitHub annotation per channel."""
import re, sys, urllib.request, urllib.parse
from concurrent.futures import ThreadPoolExecutor

UA = "Mozilla/5.0 (Linux; Android 11; MiTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
text = open(sys.argv[1], encoding="utf-8").read()
entries, cur = [], None
for line in text.splitlines():
    line = line.strip()
    if line.startswith("#EXTINF"):
        cur = {"name": line.split(",", 1)[-1].strip()}
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

with ThreadPoolExecutor(16) as p:
    for name, res in p.map(check, entries):
        print(f"::error::{res} :: {name}")
