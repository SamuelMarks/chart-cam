import os
import re
import sys

def get_kdoc_coverage(source_dirs):
    total_declarations = 0
    documented_declarations = 0
    missing = []

    decl_pattern = re.compile(r'^\s*(?:(?:public|protected|override|abstract|open|suspend|inline|data|value)\s+)*(class|interface|object|fun)\s+([a-zA-Z0-9_<>]+)')

    for d in source_dirs:
        if not os.path.exists(d): continue
        for root, _, files in os.walk(d):
            if "Test" in root or "test" in root: continue
            for file in files:
                if file.endswith(".kt"):
                    path = os.path.join(root, file)
                    with open(path, "r", encoding="utf-8") as f:
                        lines = f.readlines()
                        
                    for i, line in enumerate(lines):
                        # ignore "actual" or "private" or "internal"
                        if "actual " in line or "private " in line or "internal " in line:
                            continue
                            
                        match = decl_pattern.match(line)
                        if match:
                            total_declarations += 1
                            j = i - 1
                            is_documented = False
                            while j >= 0:
                                check_line = lines[j].strip()
                                if check_line == "" or check_line.startswith("@"):
                                    j -= 1
                                    continue
                                if check_line.endswith("*/"):
                                    is_documented = True
                                break
                            
                            if is_documented:
                                documented_declarations += 1
                            else:
                                missing.append(f"{path}:{i+1} {line.strip()}")

    if total_declarations == 0:
        return 100.0, missing
    return (documented_declarations / total_declarations) * 100.0, missing

def update_readme(readme_path, doc_cov):
    if not os.path.exists(readme_path):
        content = ""
    else:
        with open(readme_path, "r", encoding="utf-8") as f:
            content = f.read()

    doc_badge = f"![Doc Coverage](https://img.shields.io/badge/Doc%20Coverage-{doc_cov:.1f}%25-brightgreen)"

    if "![Doc Coverage]" in content:
        content = re.sub(r"!\[Doc Coverage\]\(.+?\)", doc_badge, content)
    else:
        content = doc_badge + "\n" + content

    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(content)

if __name__ == "__main__":
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    readme_path = os.path.join(project_root, "README.md")
    
    dirs = [
        os.path.join(project_root, "chartCam/src/commonMain/kotlin"),
        os.path.join(project_root, "chartCam/src/androidMain/kotlin"),
        os.path.join(project_root, "chartCam/src/iosMain/kotlin"),
        os.path.join(project_root, "chartCam/src/jvmMain/kotlin"),
        os.path.join(project_root, "chartCam/src/jsMain/kotlin"),
        os.path.join(project_root, "chartCam/src/wasmJsMain/kotlin"),
        os.path.join(project_root, "chartCam/src/webMain/kotlin"),
    ]

    doc_cov, missing = get_kdoc_coverage(dirs)
    update_readme(readme_path, doc_cov)

    print(f"Doc Coverage: {doc_cov:.1f}%")
    if doc_cov < 100.0:
        print("Doc coverage is below 100.0%. Please add missing KDocs:")
        for m in missing:
            print(m)
        sys.exit(1)
    
    sys.exit(0)
