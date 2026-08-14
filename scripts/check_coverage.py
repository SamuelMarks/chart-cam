"""
This module provides a script to calculate the KDoc documentation coverage for a
Kotlin project. It scans specified source directories, identifies Kotlin declarations
(classes, interfaces, objects, and functions), and checks if they are preceded by
a valid KDoc block. It also verifies that each file starts with a `@file` KDoc tag.
The script outputs the overall coverage percentage and updates the corresponding
badge in the project's README.md file.
"""

import os
import re
import sys


def get_kdoc_coverage(source_dirs):
    """
    Calculate the KDoc documentation coverage for Kotlin source files.

    This function scans the provided directories for `.kt` files. For each file, it
    first checks if a `@file` KDoc tag exists within the first 15 lines. Then, it
    uses a regular expression to find all major Kotlin declarations (classes,
    interfaces, objects, and functions). For every declaration found, it checks
    if the preceding lines contain a valid KDoc block (ending with `*/`).

    :param source_dirs: A list of directory paths to scan for Kotlin source files.
    :type source_dirs: list[str]
    :return: A tuple containing two elements:
             - The calculated documentation coverage as a float percentage (0.0 to 100.0).
             - A list of string messages detailing the locations of missing documentation.
    :rtype: tuple[float, list[str]]
    """
    total_declarations = 0
    documented_declarations = 0
    missing = []

    # regex to match class, interface, object, fun (including private/internal)
    decl_pattern = re.compile(
        r"^\s*(?:(?:public|protected|private|internal|override|abstract|open|suspend|inline|data|value|expect|actual)\s+)*(class|interface|object|fun)\s+([a-zA-Z0-9_<>.]+)"
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

                    has_file_doc = False
                    for i in range(min(15, len(lines))):
                        if "@file" in lines[i]:
                            has_file_doc = True
                            break
                    if not has_file_doc:
                        total_declarations += 1
                        missing.append(f"{path}:1 file missing @file KDoc")
                    else:
                        total_declarations += 1
                        documented_declarations += 1

                    for i, line in enumerate(lines):
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
                                missing.append(f"{path}:{i + 1} {line.strip()}")

    if total_declarations == 0:
        return 100.0, missing
    return (documented_declarations / total_declarations) * 100.0, missing


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

    doc_cov, missing = get_kdoc_coverage(dirs)

    print(f"Doc Coverage: {doc_cov:.1f}%")

    # Update README.md
    readme_path = os.path.join(project_root, "README.md")
    if os.path.exists(readme_path):
        with open(readme_path, "r", encoding="utf-8") as f:
            readme_content = f.read()

        color = (
            "brightgreen"
            if doc_cov >= 100.0
            else ("yellow" if doc_cov >= 80.0 else "red")
        )

        # Regex to match the Doc Coverage badge
        new_readme_content = re.sub(
            r"!\[Doc Coverage\]\(https://img\.shields\.io/badge/Doc%20Coverage-[0-9.]+%(?:25|)-[a-zA-Z]+\)",
            f"![Doc Coverage](https://img.shields.io/badge/Doc%20Coverage-{doc_cov:.1f}%25-{color})",
            readme_content,
        )

        if readme_content != new_readme_content:
            with open(readme_path, "w", encoding="utf-8") as f:
                f.write(new_readme_content)
            print("Updated README.md doc coverage badge.")

    if doc_cov < 100.0:
        print("Doc coverage is below 100.0%. Please add missing KDocs:")
        for m in missing:
            print(m)
        sys.exit(1)

    sys.exit(0)
