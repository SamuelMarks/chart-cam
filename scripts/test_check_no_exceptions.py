"""
Unit tests for `scripts/check_no_exceptions.py`.

Verifies:
- Stripping of comments, block comments, and string literals.
- Identification of forbidden exception constructs (`throw`, `catch`, `try`).
- Allow-exception exemptions (`// allow-exception`, `@Suppress("ForbiddenException")`).
- Parsing of unified diff additions and line numbers.
- File scanning logic and non-Kotlin file skipping.
- Fallback file reading for untracked files.
"""

import os
import tempfile
import unittest
from scripts.check_no_exceptions import (
    check_files,
    get_added_lines_for_file,
    get_all_staged_kotlin_files,
    has_forbidden_exception,
    is_line_exempted,
    parse_diff_added_lines,
    strip_comments_and_strings,
)


class TestCheckNoExceptions(unittest.TestCase):
    """
    Test suite for exception-prevention pre-commit hook utilities.
    """

    def test_strip_comments_and_strings(self):
        """
        Verify that string literals, char literals, and comments are stripped.
        """
        self.assertEqual(
            strip_comments_and_strings('val msg = "throw error"'),
            'val msg = ""',
        )
        self.assertEqual(
            strip_comments_and_strings('val multiline = """try { something }"""'),
            'val multiline = ""',
        )
        self.assertEqual(
            strip_comments_and_strings("val c = 't'"),
            "val c = ''",
        )
        self.assertEqual(
            strip_comments_and_strings("val x = 1 // try something"),
            "val x = 1 ",
        )
        self.assertEqual(
            strip_comments_and_strings("val x = /* catch me */ 2"),
            "val x =  2",
        )

    def test_has_forbidden_exception_throw(self):
        """
        Verify that throw statements are correctly identified.
        """
        forbidden, keyword = has_forbidden_exception("throw IllegalStateException()")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "throw")

        forbidden, keyword = has_forbidden_exception("throw e")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "throw")

    def test_has_forbidden_exception_try(self):
        """
        Verify that try blocks are correctly identified.
        """
        forbidden, keyword = has_forbidden_exception("try {")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "try")

        forbidden, keyword = has_forbidden_exception("return try {")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "try")

    def test_has_forbidden_exception_catch(self):
        """
        Verify that catch clauses are correctly identified.
        """
        forbidden, keyword = has_forbidden_exception("} catch (e: Exception) {")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "catch")

        forbidden, keyword = has_forbidden_exception("} catch (_: Throwable) {")
        self.assertTrue(forbidden)
        self.assertEqual(keyword, "catch")

    def test_has_forbidden_exception_clean_code(self):
        """
        Verify that safe code using Result, channels, and variables is permitted.
        """
        self.assertEqual(
            has_forbidden_exception("val result = Result.success(data)"),
            (False, None),
        )
        self.assertEqual(
            has_forbidden_exception("val result = Result.failure(error)"),
            (False, None),
        )
        self.assertEqual(
            has_forbidden_exception("channel.trySend(item)"),
            (False, None),
        )
        self.assertEqual(
            has_forbidden_exception("val industryStandard = true"),
            (False, None),
        )
        self.assertEqual(
            has_forbidden_exception("fun retryOperation() = 42"),
            (False, None),
        )

    def test_has_forbidden_exception_allow_exemption(self):
        """
        Verify that explicit exemption annotations and comments bypass the check.
        """
        self.assertEqual(
            has_forbidden_exception("throw IllegalStateException() // allow-exception"),
            (False, None),
        )
        self.assertEqual(
            has_forbidden_exception(
                '@Suppress("ForbiddenException") throw IllegalStateException()'
            ),
            (False, None),
        )

    def test_parse_diff_added_lines(self):
        """
        Verify parsing of Git unified diff output hunks.
        """
        diff = "@@ -10,3 +10,4 @@\n val a = 1\n+val b = 2\n+val c = 3\n val d = 4\n"
        added = parse_diff_added_lines(diff)
        self.assertEqual(len(added), 2)
        self.assertEqual(added[0], (11, "val b = 2"))
        self.assertEqual(added[1], (12, "val c = 3"))

    def test_check_files_clean(self):
        """
        Verify that clean Kotlin files return False (no violations).
        """
        with tempfile.NamedTemporaryFile(suffix=".kt", mode="w", delete=False) as f:
            f.write("fun hello(): String = 'Hello World'\n")
            filepath = f.name

        try:
            self.assertFalse(check_files([filepath]))
        finally:
            if os.path.exists(filepath):
                os.remove(filepath)

    def test_check_files_with_violations(self):
        """
        Verify that files introducing forbidden exceptions return True.
        """
        with tempfile.NamedTemporaryFile(suffix=".kt", mode="w", delete=False) as f:
            f.write("fun bad(): Unit {\n    throw RuntimeException()\n}\n")
            filepath = f.name

        try:
            self.assertTrue(check_files([filepath]))
        finally:
            if os.path.exists(filepath):
                os.remove(filepath)

    def test_check_files_skips_non_kt(self):
        """
        Verify that non-Kotlin files (e.g. .py or .md) are ignored.
        """
        with tempfile.NamedTemporaryFile(suffix=".py", mode="w", delete=False) as f:
            f.write("raise ValueError('test')\n")
            filepath = f.name

        try:
            self.assertFalse(check_files([filepath]))
        finally:
            if os.path.exists(filepath):
                os.remove(filepath)

    def test_get_added_lines_for_file_nonexistent(self):
        """
        Verify that non-existent file paths return an empty list.
        """
        self.assertEqual(get_added_lines_for_file("/non/existent/file.kt"), [])

    def test_get_all_staged_kotlin_files(self):
        """
        Verify that staged discovery runs without unhandled exceptions.
        """
        result = get_all_staged_kotlin_files()
        self.assertIsInstance(result, list)

    def test_is_line_exempted_direct_comment_and_annotation(self):
        """
        Verify that line-level comment and annotation markers are exempted.
        """
        self.assertTrue(is_line_exempted("throw RuntimeException() // allow-exception"))
        self.assertTrue(
            is_line_exempted('@Suppress("ForbiddenException") throw RuntimeException()')
        )
        self.assertFalse(is_line_exempted("throw RuntimeException()"))

    def test_is_line_exempted_preceding_line(self):
        """
        Verify that preceding line comment triggers exemption.
        """
        file_lines = ["// allow-exception\n", "throw RuntimeException()\n"]
        self.assertTrue(
            is_line_exempted(
                line="throw RuntimeException()",
                filepath="dummy.kt",
                lineno=2,
                file_lines=file_lines,
            )
        )

    def test_is_line_exempted_file_level_suppress(self):
        """
        Verify that file-level suppression exempts all lines in the file.
        """
        file_lines = [
            '@file:Suppress("ForbiddenException")\n',
            "package io.test\n",
            "fun bad() {\n",
            "    throw RuntimeException()\n",
            "}\n",
        ]
        self.assertTrue(
            is_line_exempted(
                line="    throw RuntimeException()",
                filepath="dummy.kt",
                lineno=4,
                file_lines=file_lines,
            )
        )

    def test_is_line_exempted_enclosing_function_suppress(self):
        """
        Verify that enclosing function-level suppression exempts contained lines.
        """
        file_lines = [
            "package io.test\n",
            '@Suppress("ForbiddenException", "TooGenericExceptionCaught")\n',
            "fun bad() {\n",
            "    throw RuntimeException()\n",
            "}\n",
        ]
        self.assertTrue(
            is_line_exempted(
                line="    throw RuntimeException()",
                filepath="dummy.kt",
                lineno=4,
                file_lines=file_lines,
            )
        )

    def test_check_files_with_exempted_function(self):
        """
        Verify that a Kotlin file with an exempted function passes check_files.
        """
        code = (
            '@Suppress("ForbiddenException")\n'
            "fun safe() {\n"
            "    try {\n"
            "        val x = 1\n"
            "    } catch (e: Exception) {\n"
            "        val y = 2\n"
            "    }\n"
            "}\n"
        )
        with tempfile.NamedTemporaryFile(suffix=".kt", mode="w", delete=False) as f:
            f.write(code)
            filepath = f.name

        try:
            self.assertFalse(check_files([filepath]))
        finally:
            if os.path.exists(filepath):
                os.remove(filepath)


if __name__ == "__main__":
    unittest.main()
