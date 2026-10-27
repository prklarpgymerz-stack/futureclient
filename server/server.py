# Tiny presence server for Future Client logos. Python 3, no dependencies.
#   python3 server.py          -> listens on 0.0.0.0:8765
# Then set FutureNet.API = "http://YOUR_IP_OR_DOMAIN:8765" and rebuild the mod.
import re, time
from http.server import BaseHTTPRequestHandler, HTTPServer

seen = {}
UUID_RE = re.compile(r'^[0-9a-f-]{32,36}$')

class H(BaseHTTPRequestHandler):
    def do_POST(self):
        n = min(int(self.headers.get('Content-Length', 0)), 64)
        uid = self.rfile.read(n).decode(errors='ignore').strip().lower()
        if UUID_RE.match(uid):
            seen[uid] = time.time()
        self.send_response(204); self.end_headers()

    def do_GET(self):
        now = time.time()
        for u in [u for u, t in seen.items() if now - t > 150]:
            del seen[u]
        body = "\n".join(seen).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'text/plain')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers(); self.wfile.write(body)

    def log_message(self, *a): pass

HTTPServer(('0.0.0.0', 8765), H).serve_forever()
