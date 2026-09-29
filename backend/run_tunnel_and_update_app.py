import os
import re
import subprocess
import sys

def update_retrofit_client(tunnel_url):
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    retrofit_file = os.path.join(
        project_root,
        "app", "src", "main", "java", "com", "aula", "virtual", "data", "RetrofitClient.java"
    )

    if not os.path.exists(retrofit_file):
        print(f"[!] No se encontro RetrofitClient.java en: {retrofit_file}")
        return False

    with open(retrofit_file, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = r'private\s+static\s+final\s+String\s+BASE_URL\s*=\s*"(https?://[^"]+)";'
    new_base_url = f'private static final String BASE_URL = "{tunnel_url}";'

    if re.search(pattern, content):
        new_content = re.sub(pattern, new_base_url, content)
        with open(retrofit_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print("\n===================================================")
        print("[SUCCESS] RetrofitClient.java actualizado automaticamente!")
        print(f"[BASE_URL] {tunnel_url}")
        print("===================================================\n")
        return True
    else:
        print("[!] No se pudo encontrar el patron BASE_URL en RetrofitClient.java")
        return False

def main():
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    cloudflared_exe = os.path.join(backend_dir, "cloudflared.exe")

    if not os.path.exists(cloudflared_exe):
        cloudflared_exe = "cloudflared"

    cmd = [
        cloudflared_exe, "tunnel",
        "--protocol", "http2",
        "--edge-ip-version", "4",
        "--no-autoupdate",
        "--url", "http://127.0.0.1:8000"
    ]

    print("===================================================")
    print("  AULA VIRTUAL - TUNNEL Y ACTUALIZACION AUTOMATICA")
    print("===================================================")
    print("[i] Iniciando Cloudflare Tunnel...\n")

    process = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
        universal_newlines=True
    )

    url_updated = False
    url_pattern = re.compile(r'https://[a-zA-Z0-9\-]+\.trycloudflare\.com')

    for line in process.stdout:
        print(line, end="")
        if not url_updated:
            match = url_pattern.search(line)
            if match:
                tunnel_url = match.group(0)
                if not tunnel_url.endswith("/"):
                    tunnel_url += "/"
                url_updated = update_retrofit_client(tunnel_url)

    process.wait()

if __name__ == "__main__":
    main()
