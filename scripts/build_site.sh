#!/bin/bash
set -e

# Change to the project root directory
cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

echo "=== Building ChartCam Site ==="

# 1. Build the WasmJS App
echo "--- Building Wasm Web App ---"
./gradlew :chartCam:wasmJsBrowserDistribution

# 2. Build API Docs
echo "--- Building API Docs with Dokka ---"
./gradlew :chartCam:dokkaHtml

# 3. Prepare the output directory
OUT_DIR="site_dist"
echo "--- Preparing $OUT_DIR directory ---"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# 4. Copy the landing page (HTML/CSS)
echo "--- Copying landing page ---"
cp -r site/* "$OUT_DIR/"

# 5. Copy Web App
echo "--- Copying Wasm App ---"
mkdir -p "$OUT_DIR/wasmJs"
if [ -d "chartCam/build/dist/wasmJs/productionExecutable" ]; then
    cp -r chartCam/build/dist/wasmJs/productionExecutable/* "$OUT_DIR/wasmJs/"
else
    echo "Warning: Wasm build output not found in expected directory."
fi

# 6. Copy API Docs
echo "--- Copying API Docs ---"
mkdir -p "$OUT_DIR/docs"
if [ -d "chartCam/build/dokka/html" ]; then
    cp -r chartCam/build/dokka/html/* "$OUT_DIR/docs/"
else
    echo "Warning: Dokka output not found."
fi

# 7. Convert Markdown files to HTML
echo "--- Rendering Markdown files to HTML ---"
# Install python markdown package if not exists
python3 -m pip install -q Markdown

cat << 'EOF' > render_md.py
import os
import markdown
import glob

# Wrap the markdown in our site's HTML template
template = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title} - ChartCam Docs</title>
    <!-- Use relative path to style.css based on depth or absolute path if hosted at root -->
    <link rel="stylesheet" href="/style.css">
    <style>
        .markdown-body { padding: 2rem; max-width: 800px; margin: 0 auto; }
        .markdown-body img { max-width: 100%; }
        .back-link { display: inline-block; margin-bottom: 2rem; }
    </style>
</head>
<body>
    <header>
        <div class="container">
            <h1>ChartCam Documentation</h1>
            <a href="/index.html" class="button outline" style="margin-top: 1rem; display: inline-block;">Back to Home</a>
        </div>
    </header>
    <main class="container markdown-body">
        {content}
    </main>
    <footer>
        <div class="container">
            <p>&copy; 2026 Samuel Marks. All rights reserved.</p>
        </div>
    </footer>
</body>
</html>
"""

def process_md_files(source_dirs, out_dir):
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    md_files = []
    for item in source_dirs:
        if os.path.isdir(item):
            for f in os.listdir(item):
                if f.endswith(".md"):
                    md_files.append((os.path.join(item, f), f))
        elif os.path.isfile(item) and item.endswith(".md"):
            md_files.append((item, os.path.basename(item)))

    for md_path, md_name in md_files:
        with open(md_path, "r", encoding="utf-8") as f:
            text = f.read()

        # Extra extensions for tables, code highlighting, etc.
        html_content = markdown.markdown(text, extensions=['extra', 'codehilite', 'tables', 'toc'])
        filename = md_name.replace(".md", ".html")

        # Simple title extraction
        title = filename.replace(".html", "")
        first_line = text.strip().split('\n')[0]
        if first_line.startswith("# "):
            title = first_line[2:]

        final_html = template.replace("{title}", title).replace("{content}", html_content)

        with open(os.path.join(out_dir, filename), "w", encoding="utf-8") as f:
            f.write(final_html)

# Convert root MD files and docs/ MD files. Output to site_dist/guides
process_md_files(["README.md", "USAGE.md", "SECURITY.md", "HOW_TO_RELEASE.md", "docs"], "site_dist/guides")
EOF

python3 render_md.py
rm render_md.py

# Update the api-docs link in the main index.html to point to the Dokka index
# And maybe add a section for guides
cat << 'EOF' > "$OUT_DIR/index.html"
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="ChartCam - Health Platform Application & Documentation">
    <title>ChartCam</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header>
        <div class="container">
            <h1>ChartCam</h1>
            <p>Your Health Platform Application</p>
        </div>
    </header>

    <main class="container">
        <section>
            <h2>Welcome</h2>
            <p>
                ChartCam is a cross-platform application built with Kotlin Multiplatform.
                Explore our web application or dive into our developer documentation to learn more about the project architecture, APIs, and workflows.
            </p>

            <div class="button-group">
                <a href="wasmJs/index.html" class="button">Launch Web App</a>
                <a href="docs/index.html" class="button outline">API Documentation</a>
            </div>

            <h3>Guides & Documentation</h3>
            <ul>
                <li><a href="guides/README.html">ReadMe</a></li>
                <li><a href="guides/USAGE.html">Usage Guide</a></li>
                <li><a href="guides/SECURITY.html">Security</a></li>
                <li><a href="guides/HOW_TO_RELEASE.html">How To Release</a></li>
                <li><a href="guides/NAVIGATION.html">Navigation Specs</a></li>
            </ul>
        </section>

        <section class="features">
            <div class="feature-card">
                <h3>Cross Platform</h3>
                <p>Built using Kotlin Multiplatform targeting Android, iOS, JVM, and Web (Wasm).</p>
            </div>
            <div class="feature-card">
                <h3>Open Documentation</h3>
                <p>All API designs and markdown specifications are deeply integrated.</p>
            </div>
        </section>
    </main>

    <footer>
        <div class="container">
            <p>&copy; 2026 Samuel Marks. All rights reserved.</p>
        </div>
    </footer>
</body>
</html>
EOF

cat << 'EOF' > "$OUT_DIR/serve.py"
import http.server
import socketserver

PORT = 8000
Handler = http.server.SimpleHTTPRequestHandler

# Ensure the .wasm MIME type is explicitly set
Handler.extensions_map.update({
    ".wasm": "application/wasm",
})

with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Serving at http://localhost:{PORT}")
    httpd.serve_forever()
EOF

echo "=== Build Complete! === "
echo "Your generated site is located in the '$OUT_DIR/' directory."
echo "You can test it locally. E.g., by running:"
echo "  python3 -m http.server -d $OUT_DIR"
