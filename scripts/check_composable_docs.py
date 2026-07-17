import os
import re
import sys

def check_composable_docs(source_dirs):
    missing = []
    func_pattern = re.compile(r'^\s*(?:(?:public|protected|internal|private)\s+)*fun\s+([a-zA-Z0-9_<>]+)\s*\(')
    param_pattern = re.compile(r'@param\s+([a-zA-Z0-9_]+)')

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
                        if "@Composable" in line:
                            # Look ahead for fun
                            k = i + 1
                            while k < len(lines) and not func_pattern.match(lines[k]):
                                k += 1
                                
                            if k < len(lines):
                                func_match = func_pattern.match(lines[k])
                                func_name = func_match.group(1)
                                
                                # Find parameters
                                # We'll do a naive parse until closing paren.
                                params_str = ""
                                m = k
                                while m < len(lines):
                                    params_str += lines[m]
                                    if ')' in lines[m]:
                                        break
                                    m += 1
                                
                                # Roughly extract param names
                                raw_params = re.findall(r'([a-zA-Z0-9_]+)\s*:\s*[A-Z]', params_str)
                                
                                # Check doc above @Composable
                                j = i - 1
                                is_documented = False
                                doc_block = []
                                while j >= 0:
                                    check_line = lines[j].strip()
                                    if check_line == "" or check_line.startswith("@"):
                                        j -= 1
                                        continue
                                    if check_line.endswith("*/"):
                                        is_documented = True
                                        m = j
                                        while m >= 0:
                                            doc_block.insert(0, lines[m])
                                            if lines[m].strip().startswith("/**"):
                                                break
                                            m -= 1
                                        break
                                    break
                                
                                if not is_documented:
                                    missing.append(f"{path}:{k+1} {func_name} missing KDoc")
                                else:
                                    doc_str = "".join(doc_block)
                                    doc_params = param_pattern.findall(doc_str)
                                    for p in raw_params:
                                        if p not in doc_params and p != "modifier" and p != "Modifier": # sometimes modifier is lower/upper
                                            missing.append(f"{path}:{k+1} {func_name} missing @param {p}")
                                            
    return missing

if __name__ == "__main__":
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    dirs = [
        os.path.join(project_root, "chartCam/src/commonMain/kotlin"),
        os.path.join(project_root, "chartCam/src/androidMain/kotlin"),
        os.path.join(project_root, "chartCam/src/iosMain/kotlin"),
        os.path.join(project_root, "chartCam/src/jvmMain/kotlin"),
        os.path.join(project_root, "chartCam/src/jsMain/kotlin"),
        os.path.join(project_root, "chartCam/src/wasmJsMain/kotlin"),
        os.path.join(project_root, "chartCam/src/webMain/kotlin"),
    ]
    missing = check_composable_docs(dirs)
    if missing:
        print("Missing Composable KDocs:")
        for m in missing:
            print(m)
        sys.exit(1)
    
    print("All Composables are documented.")
    sys.exit(0)
