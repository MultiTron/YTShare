import http.server
import socketserver
import webbrowser
import socket
from zeroconf import ServiceInfo, Zeroconf

PORT = 7296
SERVICE_NAME = "YTShareService"
SERVICE_TYPE = "_http._tcp.local."

class ShareHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith("/Share?link="):
            youtube_url = self.path.split("/Share?link=")[-1]
            youtube_url = "https://" + youtube_url
            print(f"Opening {youtube_url}")
            webbrowser.open(youtube_url)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"Video opened successfully.")
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Invalid request.")

class BonjourService:
    def __init__(self, name=SERVICE_NAME, regtype=SERVICE_TYPE, port=PORT):
        self.name = name
        self.regtype = regtype
        self.port = port
        self.zeroconf = Zeroconf()
        self.register()
    
    def register(self):
        service_info = ServiceInfo(
            self.regtype,
            f"{self.name}.{self.regtype}",
            addresses=[socket.inet_aton(socket.gethostbyname(socket.gethostname()))],
            port=self.port,
            properties={}
        )
        self.zeroconf.register_service(service_info)
        print(f"Service registered: {self.name}.{self.regtype}")

if __name__ == "__main__":
    handler = ShareHandler
    httpd = socketserver.TCPServer(("", PORT), handler)
    BonjourService()
    print(f"Serving on port {PORT}")
    httpd.serve_forever()
