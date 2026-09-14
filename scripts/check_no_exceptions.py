"""
This module provides a pre-commit hook script to prevent new exception-using code
(such as `throw`, `try`, and `catch` statements) from entering the codebase.

It inspects staged Git diffs (or specified Kotlin files) for newly added lines
containing forbidden exception constructs, enforcing the use of `kotlin.Result`
and `runSuspendCatching` instead of exceptions.
"""

import os
import re
import subprocess
import sys


def strip_comments_and_strings(line):
    """
    Remove string literals and comments from a single line of Kotlin code.

    This function strips single-line comments (`//...`), block comments
    (`/* ... */` on the same line), double-quoted strings, triple-quoted strings,
    and character literals to prevent false positives when keywords appear inside
    strings or documentation comments.

    :param line: The raw source code line to sanitize.
    :type line: str
    :return: The sanitized source code line free of strings and comments.
    :rtype: str
    """
    # Remove triple-quoted strings
    line = re.sub(r'""".*?"""', '""', line)
    # Remove standard double-quoted strings
    line = re.sub(r'"[^"\n]*"', '""', line)
    # Remove char literals
    line = re.sub(r"'[^'\n]*'", "''", line)
    # Remove inline block comments
    line = re.sub(r"/\*.*?\*/", "", line)
    # Remove trailing single-line comments
    line = re.sub(r"//.*$", "", line)
    return line


def has_forbidden_exception(line):
    """
    Check if a sanitized line of Kotlin code contains forbidden exception keywords.

    Forbidden constructs include:
    - `throw` statements (e.g. `throw Exception(...)`, `throw e`)
    - `catch` clauses (e.g. `catch (e: Exception)`, `catch (_: Throwable)`)
    - `try` blocks (e.g. `try {`, `return try {`)

    Exemptions can be explicitly granted on a line using `// allow-exception` or
    `@Suppress("ForbiddenException")`.

    :param line: The raw Kotlin code line to evaluate.
    :type line: str
    :return: A tuple where the first element is a boolean indicating if a forbidden
             construct was found, and the second element is the matched keyword or None.
    :rtype: tuple[bool, str | None]
    """
    # Check for explicit exemption annotations or markers
    if "// allow-exception" in line or '@Suppress("ForbiddenException")' in line:
        return False, None

    sanitized = strip_comments_and_strings(line).strip()
    if not sanitized:
        return False, None

    # Check for throw statement
    if re.search(r"\bthrow\b", sanitized):
        return True, "throw"

    # Check for catch statement
    if re.search(r"\bcatch\s*(\(|\{|$)", sanitized):
        return True, "catch"

    # Check for try statement
    if re.search(r"\btry\s*(\{|$)", sanitized):
        return True, "try"

    return False, None


def parse_diff_added_lines(diff_text):
    """
    Parse unified diff output and extract newly added lines with their line numbers.

    :param diff_text: The raw unified diff text (e.g. from `git diff -U0`).
    :type diff_text: str
    :return: A list of tuples containing the 1-based target line number and the line content.
    :rtype: list[tuple[int, str]]
    """
    added_lines = []
    current_line = 0

    # Hunk header format: @@ -from,count +to,count @@ or @@ -from +to @@
    hunk_pattern = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")

    for line in diff_text.splitlines():
        if line.startswith("@@"):
            match = hunk_pattern.match(line)
            if match:
                current_line = int(match.group(1))
        elif line.startswith("+") and not line.startswith("+++"):
            added_content = line[1:]
            added_lines.append((current_line, added_content))
            current_line += 1
        elif not line.startswith("-"):
            current_line += 1

    return added_lines


def get_added_lines_for_file(filepath):
    """
    Retrieve newly added lines for a specified file using Git diff or local read.

    Inspects working tree diff against HEAD (`git diff -U0 HEAD`). If Git is
    unavailable or the file is untracked, reads the file content as newly added.

    :param filepath: Path to the Kotlin source file to inspect.
    :type filepath: str
    :return: A list of tuples containing line numbers and added line strings.
    :rtype: list[tuple[int, str]]
    """
    if not os.path.exists(filepath):
        return []

    # Check uncommitted changes against HEAD
    try:
        proc = subprocess.run(
            [
                "git",
                "--no-pager",
                "diff",
                "--no-ext-diff",
                "-U0",
                "HEAD",
                "--",
                filepath,
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        if proc.returncode == 0:
            if proc.stdout.strip():
                return parse_diff_added_lines(proc.stdout)
            return []
    except FileNotFoundError:
        pass

    # Fallback: Untracked new file or non-git environment, check all lines
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return [(idx + 1, line.rstrip()) for idx, line in enumerate(f)]
    except (IOError, UnicodeDecodeError):
        return []


def is_line_exempted(line, filepath="", lineno=0, file_lines=None):
    """
    Check if a code line or its enclosing scope is explicitly exempted from exception checks.

    Exemptions are recognized when:
    - The line itself contains `// allow-exception` or `@Suppress("ForbiddenException")`
    - The preceding line contains `// allow-exception` or `@Suppress("ForbiddenException")`
    - The file contains a file-level `@file:Suppress("ForbiddenException")`
    - An enclosing function or class is annotated with `@Suppress("ForbiddenException", ...)`

    :param line: The code line content.
    :type line: str
    :param filepath: Optional path to the file.
    :type filepath: str
    :param lineno: 1-based line number of the code line.
    :type lineno: int
    :param file_lines: Optional cached list of all lines in the file.
    :type file_lines: list[str] | None
    :return: True if the construct is exempted, False otherwise.
    :rtype: bool
    """
    if "// allow-exception" in line or (
        "@Suppress(" in line and "ForbiddenException" in line
    ):
        return True

    if file_lines is None and filepath and os.path.exists(filepath):
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                file_lines = f.readlines()
        except (IOError, UnicodeDecodeError):
            file_lines = None

    if file_lines is not None:
        # Check file-level suppression
        for fline in file_lines[:30]:
            if "@file:Suppress(" in fline and "ForbiddenException" in fline:
                return True

        # Check immediately preceding line (1-based lineno, so lineno - 2 is prev index)
        if 0 <= lineno - 2 < len(file_lines):
            prev = file_lines[lineno - 2].strip()
            if "// allow-exception" in prev or (
                "@Suppress(" in prev and "ForbiddenException" in prev
            ):
                return True

        # Scan upward from current line to check enclosing function or class annotations
        if 0 < lineno <= len(file_lines):
            brace_balance = 0
            for idx in range(lineno - 1, -1, -1):
                cur = file_lines[idx]
                brace_balance += cur.count("}") - cur.count("{")
                if brace_balance < 0:
                    for a_idx in range(idx, max(-1, idx - 10), -1):
                        a_line = file_lines[a_idx].strip()
                        if "@Suppress(" in a_line and "ForbiddenException" in a_line:
                            return True
                        if (
                            a_line.startswith("fun ")
                            or a_line.startswith("inline fun ")
                            or (
                                a_line.startswith("class ")
                                or a_line.startswith("object ")
                            )
                        ):
                            if a_idx > 0:
                                prev_decl = file_lines[a_idx - 1].strip()
                                if (
                                    "@Suppress(" in prev_decl
                                    and "ForbiddenException" in prev_decl
                                ):
                                    return True
                            break
                    brace_balance = 0
    return False


def check_files(files_to_check):
    """
    Scan given files for any newly added exception constructs.

    Only `.kt` Kotlin source files are examined. If a forbidden construct is
    detected, details are printed to standard output with recommendations to use
    `kotlin.Result` instead.

    :param files_to_check: A list of file paths to validate.
    :type files_to_check: list[str]
    :return: True if at least one forbidden exception construct was found, False otherwise.
    :rtype: bool
    """
    has_violations = False

    for filepath in files_to_check:
        if not filepath.endswith(".kt"):
            continue

        file_lines = None
        if os.path.exists(filepath):
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    file_lines = f.readlines()
            except (IOError, UnicodeDecodeError):
                file_lines = None

        added_lines = get_added_lines_for_file(filepath)
        for lineno, line in added_lines:
            if is_line_exempted(line, filepath, lineno, file_lines):
                continue
            is_forbidden, keyword = has_forbidden_exception(line)
            if is_forbidden:
                print(
                    f"ERROR: Forbidden exception construct '{keyword}' detected in {filepath}:{lineno}\n"
                    f"  Line: {line.strip()}\n"
                    f"  Recommendation: Use 'kotlin.Result' (Result.success/Result.failure) or "
                    f"'runSuspendCatching' instead of try/catch/throw."
                )
                has_violations = True

    return has_violations


def get_all_staged_kotlin_files():
    """
    Discover all staged Kotlin source files using Git status.

    :return: A list of paths to staged `.kt` files.
    :rtype: list[str]
    """
    try:
        proc = subprocess.run(
            ["git", "diff", "--cached", "--name-only", "--diff-filter=ACM"],
            capture_output=True,
            text=True,
            check=False,
        )
        if proc.returncode == 0:
            return [
                line.strip()
                for line in proc.stdout.splitlines()
                if line.strip().endswith(".kt")
            ]
    except FileNotFoundError:
        pass
    return []


if __name__ == "__main__":
    targets = sys.argv[1:]
    if not targets:
        targets = get_all_staged_kotlin_files()

    if not targets:
        # No Kotlin files to inspect
        sys.exit(0)

    if check_files(targets):
        print("\nPre-commit hook failed: New exception-using code is not permitted.")
        print(
            "Please migrate error handling to 'kotlin.Result' per the project TODO_PLAN.md."
        )
        sys.exit(1)

    sys.exit(0)
