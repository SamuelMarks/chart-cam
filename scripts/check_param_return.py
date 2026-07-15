import os
import re
import sys

def check_param_return(source_dirs):
    total_funcs = 0
    missing = []

    func_pattern = re.compile(r'^\s*(?:(?:public|protected|override|abstract|open|suspend|inline)\s+)*fun\s+([a-zA-Z0-9_<>]+)\s*\((.*?)\)(?:\s*:\s*([a-zA-Z0-9_<>?]+))?')

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
                        if "actual " in line or "private " in line or "internal " in line:
                            continue
                            
                        match = func_pattern.search(line)
                        if match:
                            func_name = match.group(1)
                            params_str = match.group(2)
                            return_type = match.group(3)
                            
                            has_params = False
                            if params_str and params_str.strip() != "":
                                has_params = True
                            
                            has_return = False
                            if return_type and return_type.strip() != "Unit":
                                has_return = True
                                
                            if not has_params and not has_return:
                                continue

                            total_funcs += 1
                            
                            # Check KDoc above
                            j = i - 1
                            doc_block = []
                            is_documented = False
                            while j >= 0:
                                check_line = lines[j].strip()
                                if check_line == "" or check_line.startswith("@"):
                                    j -= 1
                                    continue
                                if check_line.endswith("*/"):
                                    is_documented = True
                                    # Collect block
                                    k = j
                                    while k >= 0:
                                        doc_block.insert(0, lines[k])
                                        if lines[k].strip().startswith("/**"):
                                            break
                                        k -= 1
                                    break
                                break
                            
                            doc_str = "".join(doc_block)
                            
                            missing_things = []
                            if has_params:
                                # Count params (rough split by comma, ignoring nested brackets)
                                param_count = len([p for p in params_str.split(',') if ':' in p])
                                if doc_str.count('@param') < param_count and "Composable" not in "".join(lines[max(0, i-5):i]):
                                    # For composables we'll skip for this specific check if it's too noisy, but requirement says viewmodel/repository.
                                    pass
                                    
                            if has_return and "@return" not in doc_str:
                                missing_things.append("return")
                            
                            # Let's filter just repository and viewmodel paths
                            if "repository" in path or "viewmodel" in path:
                                if has_params:
                                    param_count = len([p for p in params_str.split(',') if ':' in p])
                                    if doc_str.count('@param') < param_count:
                                        missing_things.append("params")
                                
                                if missing_things:
                                    missing.append(f"{path}:{i+1} {func_name} missing {', '.join(missing_things)}")

    return missing

dirs = [
    "chartCam/src/commonMain/kotlin",
]
missing = check_param_return(dirs)
for m in missing:
    print(m)
