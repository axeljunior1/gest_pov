#!/usr/bin/env python3
"""Generate GUIDE_UTILISATEUR.pdf from GUIDE_UTILISATEUR.md using Edge headless."""
import os
import subprocess
import sys

try:
    import markdown
except ImportError:
    print("Install: pip install markdown", file=sys.stderr)
    sys.exit(1)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
MD_PATH = os.path.join(DOCS, "GUIDE_UTILISATEUR.md")
HTML_PATH = os.path.join(DOCS, "GUIDE_UTILISATEUR.html")
PDF_PATH = os.path.join(DOCS, "GUIDE_UTILISATEUR.pdf")

CSS = """
@page { margin: 2cm; }
body {
  font-family: 'Segoe UI', Arial, sans-serif;
  font-size: 11pt;
  line-height: 1.5;
  color: #1a1a1a;
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}
h1 { font-size: 22pt; border-bottom: 2px solid #111; padding-bottom: 0.3em; page-break-before: always; }
h1:first-of-type { page-break-before: avoid; }
h2 { font-size: 15pt; margin-top: 1.4em; color: #111; }
h3 { font-size: 12pt; margin-top: 1em; }
table { border-collapse: collapse; width: 100%; margin: 1em 0; font-size: 10pt; }
th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; }
th { background: #f5f5f5; }
blockquote { border-left: 4px solid #ddd; margin: 1em 0; padding: 0.5em 1em; color: #444; background: #fafafa; }
code { background: #f4f4f4; padding: 1px 4px; font-size: 10pt; }
hr { border: none; border-top: 1px solid #ddd; margin: 2em 0; }
p { margin: 0.6em 0; }
ul, ol { margin: 0.5em 0; padding-left: 1.5em; }
li { margin: 0.25em 0; }
"""

EDGE_PATHS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
]


def find_browser():
    for path in EDGE_PATHS:
        if os.path.isfile(path):
            return path
    return None


def main():
    if not os.path.isfile(MD_PATH):
        print(f"Missing {MD_PATH}", file=sys.stderr)
        sys.exit(1)

    with open(MD_PATH, encoding="utf-8") as f:
        md_content = f.read()

    extensions = ["tables", "fenced_code", "toc"]
    body = markdown.markdown(md_content, extensions=extensions)
    html = f"""<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <title>Guide Utilisateur Gest_POV</title>
  <style>{CSS}</style>
</head>
<body>
{body}
</body>
</html>"""

    with open(HTML_PATH, "w", encoding="utf-8") as f:
        f.write(html)

    browser = find_browser()
    if not browser:
        print("Edge/Chrome not found. HTML written:", HTML_PATH, file=sys.stderr)
        sys.exit(1)

    html_url = "file:///" + HTML_PATH.replace("\\", "/")
    cmd = [
        browser,
        "--headless=new",
        "--disable-gpu",
        f"--print-to-pdf={PDF_PATH}",
        "--no-pdf-header-footer",
        html_url,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        print(result.stderr or result.stdout, file=sys.stderr)
        sys.exit(result.returncode)

    if os.path.isfile(PDF_PATH):
        print(f"PDF generated: {PDF_PATH}")
    else:
        print("PDF generation failed", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
