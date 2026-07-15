import os
import re
import sys

def check_composable_docs(source_dirs):
    missing = []
    composable_pattern = re.compile(r'@Composable')
    func_pattern = re.compile(r'^\s*(?:(?:public|protected|internal|private)\s+)*fun\s+([a-zA-Z0-9_<>]+)\s*\(')

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
                                    doc_str = "".join(doc_block).lower()
                                    # Very basic check: just see if it mentions Modifier if the signature has it
                                    # and see if it mentions state/side-effects. We will just check if there's *any* KDoc to start with.
                                    # Since the requirement says: "Add comprehensive KDocs for all Composable functions, detailing state side-effects and Modifier behaviors"
                                    
    return missing

dirs = [
    "chartCam/src/commonMain/kotlin",
]
missing = check_composable_docs(dirs)
for m in missing:
    print(m)
