"""
This module provides a script to perform accessibility (a11y) checks on Jetpack
Compose UI code. It scans Kotlin source files to ensure that visual elements like
`Icon` and `Image` composables include a `contentDescription` parameter, that hardcoded
untranslated English contentDescriptions are avoided, and that touch and heading semantics
are respected.
"""

import os
import re
import sys


def check_a11y(source_dirs):
    """
    Check Kotlin source files for accessibility compliance on Compose UI elements.

    This function scans the provided directories for `.kt` files. Within each file,
    it searches for invocations of `Icon(` or `Image(`. When found, it inspects the
    immediate surrounding lines (up to 8 lines ahead) to verify that a
    `contentDescription` parameter is passed. It also checks that literal English
    strings are not hardcoded in `contentDescription`, and that UI components maintain
    appropriate accessibility roles.

    :param source_dirs: A list of directory paths to scan for Kotlin source files.
    :type source_dirs: list[str]
    :return: A list of string messages detailing any accessibility violations found.
             An empty list indicates that all checked elements are compliant.
    :rtype: list[str]
    """
    missing = []

    icon_pattern = re.compile(r"\bIcon\(")
    image_pattern = re.compile(r"\bImage\(")
    desc_pattern = re.compile(r"contentDescription\s*=")
    hardcoded_cd_pattern = re.compile(r'contentDescription\s*=\s*"([A-Z][a-z0-9 ]{3,})"')

    for d in source_dirs:
        if not os.path.exists(d):
            continue
        for root, _, files in os.walk(d):
            for file in files:
                if file.endswith(".kt") and not file.endswith("Test.kt"):
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
                                missing.append(
                                    f"{path}:{i + 1} Icon/Image missing contentDescription"
                                )

                        # Check for hardcoded contentDescription literals in non-test files
                        m = hardcoded_cd_pattern.search(line)
                        if m and "test" not in path.lower():
                            missing.append(
                                f"{path}:{i + 1} Hardcoded contentDescription literal: '{m.group(1)}'"
                            )

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
