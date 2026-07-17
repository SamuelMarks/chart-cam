import os
import re
import sys

def check_a11y(source_dirs):
    missing = []
    
    icon_pattern = re.compile(r'\bIcon\(')
    image_pattern = re.compile(r'\bImage\(')
    
    desc_pattern = re.compile(r'contentDescription\s*=')
    
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
                        if icon_pattern.search(line) or image_pattern.search(line):
                            found = False
                            for j in range(i, min(len(lines), i + 8)):
                                if desc_pattern.search(lines[j]):
                                    found = True
                                    break
                            if not found:
                                missing.append(f"{path}:{i+1} Icon/Image missing contentDescription")

    return missing

if __name__ == "__main__":
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    dirs = [
        os.path.join(project_root, "chartCam/src"),
    ]
    missing = check_a11y(dirs)
    if missing:
        print("A11y issues found:")
        for m in missing:
            print(m)
        sys.exit(1)
        
    print("A11y checks passed")
    sys.exit(0)
