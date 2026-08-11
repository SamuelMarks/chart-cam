import os
import re
import sys


def check_param_return(source_dirs):
    total_funcs = 0
    missing = []

    func_pattern = re.compile(
        r"^\s*(?:(?:public|protected|private|internal|override|abstract|open|suspend|inline|expect|actual)\s+)*fun\s+([a-zA-Z0-9_<>.]+)\s*\("
    )

    for d in source_dirs:
        if not os.path.exists(d):
            continue
        for root, _, files in os.walk(d):
            for file in files:
                if file.endswith(".kt"):
                    path = os.path.join(root, file)
                    with open(path, "r", encoding="utf-8") as f:
                        lines = f.readlines()

                    for i, line in enumerate(lines):
                        if "actual " in line:
                            continue

                        match = func_pattern.match(line)
                        if match:
                            func_name = match.group(1)

                            # Extract parameters and return type
                            j = i
                            sig = ""
                            paren_count = 0
                            found_first_paren = False

                            while j < len(lines):
                                char_idx = 0
                                while char_idx < len(lines[j]):
                                    c = lines[j][char_idx]
                                    sig += c
                                    if c == "(":
                                        paren_count += 1
                                        found_first_paren = True
                                    elif c == ")":
                                        paren_count -= 1

                                    if found_first_paren and paren_count == 0:
                                        break
                                    char_idx += 1

                                if found_first_paren and paren_count == 0:
                                    # Collect a bit more for the return type (up to { or = or \n)
                                    rest = lines[j][char_idx + 1 :].strip()
                                    if not rest and j + 1 < len(lines):
                                        rest = lines[j + 1].strip()

                                    sig += (
                                        " " + rest.split("{")[0].split("=")[0].strip()
                                    )
                                    break
                                j += 1

                            # check if return type
                            has_return = False
                            post_paren = sig.split(")")[-1] if ")" in sig else ""
                            if (
                                ":" in post_paren
                                and "->" not in post_paren.split(":")[0]
                            ):
                                ret_type_match = re.search(
                                    r":\s*([a-zA-Z0-9_<>?]+)", post_paren
                                )
                                if ret_type_match and ret_type_match.group(1) != "Unit":
                                    has_return = True

                            # param check
                            params_part = ""
                            if "(" in sig and ")" in sig:
                                params_part = sig[sig.find("(") + 1 : sig.rfind(")")]

                            # find all words followed by colon at the top level of params
                            # simplify by removing nested brackets <> and ()
                            clean_params = re.sub(r"<[^>]*>", "", params_part)
                            clean_params = re.sub(r"\([^)]*\)", "", clean_params)
                            raw_params = re.findall(
                                r"([a-zA-Z0-9_]+)\s*:", clean_params
                            )

                            raw_params = [
                                p for p in raw_params if p.lower() != "modifier"
                            ]

                            has_params = len(raw_params) > 0

                            if not has_params and not has_return:
                                continue

                            total_funcs += 1

                            # Check KDoc above
                            k = i - 1
                            doc_block = []
                            is_documented = False
                            while k >= 0:
                                check_line = lines[k].strip()
                                if check_line == "" or check_line.startswith("@"):
                                    k -= 1
                                    continue
                                if check_line.endswith("*/"):
                                    is_documented = True
                                    m = k
                                    while m >= 0:
                                        doc_block.insert(0, lines[m])
                                        if lines[m].strip().startswith("/**"):
                                            break
                                        m -= 1
                                    break
                                break

                            if is_documented:
                                doc_str = "".join(doc_block)
                                missing_things = []

                                for p in raw_params:
                                    if f"@param {p}" not in doc_str:
                                        missing_things.append(f"param {p}")

                                if has_return and "@return" not in doc_str:
                                    missing_things.append("return")

                                if missing_things:
                                    missing.append(
                                        f"{path}:{i + 1} {func_name} missing: {', '.join(missing_things)}"
                                    )

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
    missing = check_param_return(dirs)
    if missing:
        print("Missing @param or @return in KDocs:")
        for m in missing:
            print(m)
        sys.exit(1)

    print(f"All parameters and return types documented. ")
    sys.exit(0)
