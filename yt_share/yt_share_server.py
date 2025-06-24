import http.server
import re
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

    ipv4_regex = re.compile(r'^(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)$')

    def is_valid_ipv4(self, ip):
        return bool(self.ipv4_regex.match(ip))

    def get_all_ip_addresses(self):
        host_name = socket.gethostname()
        addresses = set()
    
        # Get all IPs associated with the host
        for info in socket.getaddrinfo(host_name, None):
            ip = info[4][0]
            if self.is_valid_ipv4(ip):  # Exclude loopback
                addresses.add(socket.inet_aton(ip))
    
        return list(addresses)
    
    def register(self):
        service_info = ServiceInfo(
            self.regtype,
            f"{self.name}.{self.regtype}",
            addresses=self.get_all_ip_addresses(),
            port=self.port,
            properties={"hostname": socket.getfqdn()},
            server=socket.gethostname(),
            interface_index=0
        )
        self.zeroconf.register_service(service_info)
        print(f"Service registered: {self.name}.{self.regtype}")

if __name__ == "__main__":
    handler = ShareHandler
    httpd = socketserver.TCPServer(("", PORT), handler)
    BonjourService()
    print(f"Serving on port {PORT}")
    httpd.serve_forever()
