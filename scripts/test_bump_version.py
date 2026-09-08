"""
Unit tests for `scripts/bump_version.py`.

Verifies:
- Semantic version parsing and patch increments.
- Error handling for invalid versions or missing files.
- Version code and build number increments.
- Modifications across Android build scripts, iOS configs, project.pbxproj,
  desktop JVM packageVersion, and the About dialog screen.
- Dry run simulations versus real modifications.
- Atomic updates and validation.
"""

import os
import shutil
import tempfile
import unittest
from scripts.bump_version import (
    bump_all_components,
    bump_semver_patch,
    detect_current_version,
    update_about_version,
    update_android_build,
    update_ios_pbxproj,
    update_ios_xcconfig,
    update_jvm_package_version,
)


class TestBumpVersion(unittest.TestCase):
    """
    Test suite for version bumping utility functions.
    """

    def test_bump_semver_patch(self):
        """
        Verify standard and edge-case semantic version patch increments.
        """
        self.assertEqual(bump_semver_patch("1.0.1"), "1.0.2")
        self.assertEqual(bump_semver_patch("1.0.9"), "1.0.10")
        self.assertEqual(bump_semver_patch("0.0.1"), "0.0.2")
        self.assertEqual(bump_semver_patch("2.1"), "2.1.1")
        self.assertEqual(bump_semver_patch("1.0.0-beta.1"), "1.0.1-beta.1")

    def test_bump_semver_patch_invalid(self):
        """
        Verify that invalid version strings raise ValueError.
        """
        with self.assertRaises(ValueError):
            bump_semver_patch("invalid_version")
        with self.assertRaises(ValueError):
            bump_semver_patch("")

    def test_detect_current_version(self):
        """
        Verify detecting current version and code from repository files.
        """
        temp_dir = tempfile.mkdtemp()
        try:
            android_dir = os.path.join(temp_dir, "androidApp")
            ios_dir = os.path.join(temp_dir, "iosApp", "Configuration")
            os.makedirs(android_dir)
            os.makedirs(ios_dir)

            android_file = os.path.join(android_dir, "build.gradle.kts")
            with open(android_file, "w", encoding="utf-8") as f:
                f.write('versionCode = 5\nversionName = "2.3.4"\n')

            ios_file = os.path.join(ios_dir, "Config.xcconfig")
            with open(ios_file, "w", encoding="utf-8") as f:
                f.write("CURRENT_PROJECT_VERSION=5\nMARKETING_VERSION=2.3.4\n")

            vname, vcode = detect_current_version(temp_dir)
            self.assertEqual(vname, "2.3.4")
            self.assertEqual(vcode, 5)
        finally:
            shutil.rmtree(temp_dir)

    def test_detect_current_version_missing_raises(self):
        """
        Verify that missing files raise a RuntimeError when detecting versions.
        """
        temp_dir = tempfile.mkdtemp()
        try:
            with self.assertRaises(RuntimeError):
                detect_current_version(temp_dir)
        finally:
            shutil.rmtree(temp_dir)

    def test_update_android_build(self):
        """
        Verify updating versionCode and versionName in androidApp/build.gradle.kts.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write(
                'defaultConfig {\n    versionCode = 2\n    versionName = "1.0.1"\n}\n'
            )
            temp_file.close()

            # Dry run: verify file is untouched
            update_android_build(temp_file.name, "1.0.2", 3, dry_run=True)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                self.assertIn("versionCode = 2", f.read())

            # Real run: verify modifications
            update_android_build(temp_file.name, "1.0.2", 3, dry_run=False)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                content = f.read()
                self.assertIn("versionCode = 3", content)
                self.assertIn('versionName = "1.0.2"', content)
        finally:
            os.remove(temp_file.name)

    def test_update_android_build_missing_fields(self):
        """
        Verify error raised when versionCode or versionName are missing.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write("no versions here\n")
            temp_file.close()

            with self.assertRaises(RuntimeError):
                update_android_build(temp_file.name, "1.0.2", 3)
        finally:
            os.remove(temp_file.name)

    def test_update_ios_xcconfig(self):
        """
        Verify updating CURRENT_PROJECT_VERSION and MARKETING_VERSION in Config.xcconfig.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write(
                "CURRENT_PROJECT_VERSION=2\nMARKETING_VERSION=1.0.1\nEXPORT_COMPLIANCE_CODE=\n"
            )
            temp_file.close()

            update_ios_xcconfig(temp_file.name, "1.0.2", 3, dry_run=False)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                content = f.read()
                self.assertIn("CURRENT_PROJECT_VERSION=3", content)
                self.assertIn("MARKETING_VERSION=1.0.2", content)
        finally:
            os.remove(temp_file.name)

    def test_update_ios_xcconfig_missing_fields(self):
        """
        Verify error raised when xcconfig does not contain required fields.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write("FOO=BAR\n")
            temp_file.close()
            with self.assertRaises(RuntimeError):
                update_ios_xcconfig(temp_file.name, "1.0.2", 3)
        finally:
            os.remove(temp_file.name)

    def test_update_ios_pbxproj(self):
        """
        Verify updating all CURRENT_PROJECT_VERSION and MARKETING_VERSION entries in project.pbxproj.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            content = (
                "buildSettings = {\n"
                "    CURRENT_PROJECT_VERSION = 2;\n"
                "    MARKETING_VERSION = 1.0.1;\n"
                "};\n"
                "buildSettings = {\n"
                "    CURRENT_PROJECT_VERSION = 2;\n"
                "    MARKETING_VERSION = 1.0.1;\n"
                "};\n"
            )
            temp_file.write(content)
            temp_file.close()

            update_ios_pbxproj(temp_file.name, "1.0.2", 3, dry_run=False)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                updated = f.read()
                self.assertEqual(updated.count("CURRENT_PROJECT_VERSION = 3;"), 2)
                self.assertEqual(updated.count("MARKETING_VERSION = 1.0.2;"), 2)
        finally:
            os.remove(temp_file.name)

    def test_update_ios_pbxproj_missing_fields(self):
        """
        Verify error raised when project.pbxproj does not contain version entries.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write("isa = PBXProject;\n")
            temp_file.close()
            with self.assertRaises(RuntimeError):
                update_ios_pbxproj(temp_file.name, "1.0.2", 3)
        finally:
            os.remove(temp_file.name)

    def test_update_jvm_package_version(self):
        """
        Verify updating packageVersion in chartCam/build.gradle.kts.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write('nativeDistributions {\n    packageVersion = "1.0.0"\n}\n')
            temp_file.close()

            update_jvm_package_version(temp_file.name, "1.0.2", dry_run=False)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                content = f.read()
                self.assertIn('packageVersion = "1.0.2"', content)
        finally:
            os.remove(temp_file.name)

    def test_update_jvm_package_version_missing_fields(self):
        """
        Verify error raised when packageVersion is not in build.gradle.kts.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write("buildscript {}\n")
            temp_file.close()
            with self.assertRaises(RuntimeError):
                update_jvm_package_version(temp_file.name, "1.0.2")
        finally:
            os.remove(temp_file.name)

    def test_update_about_version(self):
        """
        Verify updating the version string in PatientListScreen.kt About dialog.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            content = 'val fullText = stringResource(Res.string.version_text, "0.0.1 — https://healthplatform.io")\n'
            temp_file.write(content)
            temp_file.close()

            update_about_version(temp_file.name, "1.0.2", dry_run=False)
            with open(temp_file.name, "r", encoding="utf-8") as f:
                updated = f.read()
                self.assertIn(
                    'stringResource(Res.string.version_text, "1.0.2 — https://healthplatform.io")',
                    updated,
                )
        finally:
            os.remove(temp_file.name)

    def test_update_about_version_missing_pattern(self):
        """
        Verify error raised when About dialog pattern is absent.
        """
        temp_file = tempfile.NamedTemporaryFile("w+", delete=False, encoding="utf-8")
        try:
            temp_file.write("fun Screen() {}\n")
            temp_file.close()
            with self.assertRaises(RuntimeError):
                update_about_version(temp_file.name, "1.0.2")
        finally:
            os.remove(temp_file.name)

    def test_bump_all_components_integration(self):
        """
        Verify end-to-end version bumping across all repository components.
        """
        temp_dir = tempfile.mkdtemp()
        try:
            android_dir = os.path.join(temp_dir, "androidApp")
            ios_cfg_dir = os.path.join(temp_dir, "iosApp", "Configuration")
            ios_proj_dir = os.path.join(temp_dir, "iosApp", "iosApp.xcodeproj")
            jvm_dir = os.path.join(temp_dir, "chartCam")
            about_dir = os.path.join(
                temp_dir,
                "chartCam",
                "src",
                "commonMain",
                "kotlin",
                "io",
                "healthplatform",
                "chartcam",
                "ui",
            )

            os.makedirs(android_dir)
            os.makedirs(ios_cfg_dir)
            os.makedirs(ios_proj_dir)
            os.makedirs(about_dir)

            with open(os.path.join(android_dir, "build.gradle.kts"), "w") as f:
                f.write('versionCode = 2\nversionName = "1.0.1"\n')
            with open(os.path.join(ios_cfg_dir, "Config.xcconfig"), "w") as f:
                f.write("CURRENT_PROJECT_VERSION=2\nMARKETING_VERSION=1.0.1\n")
            with open(os.path.join(ios_proj_dir, "project.pbxproj"), "w") as f:
                f.write("CURRENT_PROJECT_VERSION = 2;\nMARKETING_VERSION = 1.0.1;\n")
            with open(os.path.join(jvm_dir, "build.gradle.kts"), "w") as f:
                f.write('packageVersion = "1.0.0"\n')
            with open(os.path.join(about_dir, "PatientListScreen.kt"), "w") as f:
                f.write(
                    'val fullText = stringResource(Res.string.version_text, "0.0.1 — https://healthplatform.io")\n'
                )

            # Test Dry Run
            summary_dry = bump_all_components(temp_dir, dry_run=True)
            self.assertEqual(summary_dry["previous_version"], "1.0.1")
            self.assertEqual(summary_dry["new_version"], "1.0.2")
            self.assertEqual(summary_dry["previous_build_code"], "2")
            self.assertEqual(summary_dry["new_build_code"], "3")

            # Check that files were untouched during dry run
            with open(os.path.join(android_dir, "build.gradle.kts"), "r") as f:
                self.assertIn("versionCode = 2", f.read())

            # Test Real Run
            summary_real = bump_all_components(temp_dir, dry_run=False)
            self.assertEqual(summary_real["new_version"], "1.0.2")
            self.assertEqual(summary_real["new_build_code"], "3")

            with open(os.path.join(android_dir, "build.gradle.kts"), "r") as f:
                content = f.read()
                self.assertIn("versionCode = 3", content)
                self.assertIn('versionName = "1.0.2"', content)
            with open(os.path.join(ios_cfg_dir, "Config.xcconfig"), "r") as f:
                content = f.read()
                self.assertIn("CURRENT_PROJECT_VERSION=3", content)
                self.assertIn("MARKETING_VERSION=1.0.2", content)
            with open(os.path.join(ios_proj_dir, "project.pbxproj"), "r") as f:
                content = f.read()
                self.assertIn("CURRENT_PROJECT_VERSION = 3;", content)
                self.assertIn("MARKETING_VERSION = 1.0.2;", content)
            with open(os.path.join(jvm_dir, "build.gradle.kts"), "r") as f:
                content = f.read()
                self.assertIn('packageVersion = "1.0.2"', content)
            with open(os.path.join(about_dir, "PatientListScreen.kt"), "r") as f:
                content = f.read()
                self.assertIn(
                    'stringResource(Res.string.version_text, "1.0.2 — https://healthplatform.io")',
                    content,
                )

            # Test Consecutive Bump
            summary_consecutive = bump_all_components(temp_dir, dry_run=False)
            self.assertEqual(summary_consecutive["new_version"], "1.0.3")
            self.assertEqual(summary_consecutive["new_build_code"], "4")

            with open(os.path.join(android_dir, "build.gradle.kts"), "r") as f:
                content = f.read()
                self.assertIn("versionCode = 4", content)
                self.assertIn('versionName = "1.0.3"', content)
            with open(os.path.join(about_dir, "PatientListScreen.kt"), "r") as f:
                content = f.read()
                self.assertIn(
                    'stringResource(Res.string.version_text, "1.0.3 — https://healthplatform.io")',
                    content,
                )
        finally:
            shutil.rmtree(temp_dir)


if __name__ == "__main__":
    unittest.main()
