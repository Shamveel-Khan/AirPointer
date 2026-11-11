#!/usr/bin/env python3
"""
AirPointer Desk Mouse Test - Local Web Server Runner
"""

import http.server
import socket
import socketserver
import os
import sys

PORT = 8000
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Allow cross-origin isolation and permissions for sensors
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        super().end_headers()

def main():
    local_ip = get_local_ip()

    print("=" * 64)
    print("🖱️  AirPointer Desk Mouse Kinematics Testing Server")
    print("=" * 64)
    print(f"\n📂 Serving directory: {DIRECTORY}")
    print(f"\n📱 RECOMMENDED FOR ANDROID (Zero-Config Secure Context):")
    print(f"   1. Connect your phone via USB with USB Debugging enabled")
    print(f"   2. Run this command in your computer's terminal:")
    print(f"      \033[1;32madb reverse tcp:{PORT} tcp:{PORT}\033[0m")
    print(f"   3. Open Chrome on your phone and navigate to:")
    print(f"      \033[1;36mhttp://localhost:{PORT}\033[0m (Live Tester & Recorder)")
    print(f"      \033[1;36mhttp://localhost:{PORT}/analytics.html\033[0m (Advanced Analytics Studio)\n")
    print(f"   (Chrome considers localhost a Secure Context, enabling full sensor APIs!)\n")
    print(f"🌐 DIRECT LAN ACCESS (Requires Chrome flag for insecure origins):")
    print(f"   http://{local_ip}:{PORT}")
    print("-" * 64)
    print("Press Ctrl+C to stop the server.\n")

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server.")

if __name__ == "__main__":
    main()
