"""
This module provides a script to bump the version by one patch version across all
platforms and components in the ChartCam project.

This includes:
- Android Google Play versionCode and versionName in `androidApp/build.gradle.kts`
- Apple App Store build number (CURRENT_PROJECT_VERSION) and marketing version
  (MARKETING_VERSION) in `iosApp/Configuration/Config.xcconfig` and
  `iosApp/iosApp.xcodeproj/project.pbxproj`
- Desktop/JVM packaging version (packageVersion) in `chartCam/build.gradle.kts`
- About screen version display in
  `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/ui/PatientListScreen.kt`
"""

import argparse
import os
import re
import sys
from typing import Dict, Optional, Tuple


def bump_semver_patch(version: str) -> str:
    """
    Increment the patch component of a semantic version string.

    For a version string in the format 'MAJOR.MINOR.PATCH' (or 'MAJOR.MINOR'),
    this function increments the patch number by one (assuming patch is 0 if omitted).
    Pre-release suffixes or build metadata are preserved or cleanly handled.

    :param version: The current version string (e.g., '1.0.1', '1.0', or '0.0.1').
    :type version: str
    :return: The new version string with the patch version incremented by 1 (e.g., '1.0.2').
    :rtype: str
    :raises ValueError: If the version string does not start with valid integer components.
    """
    match = re.match(r"^(\d+)\.(\d+)(?:\.(\d+))?(.*)$", version.strip())
    if not match:
        raise ValueError(f"Invalid semantic version format: '{version}'")

    major = int(match.group(1))
    minor = int(match.group(2))
    patch = int(match.group(3)) if match.group(3) is not None else 0
    extra = match.group(4) or ""

    new_patch = patch + 1
    return f"{major}.{minor}.{new_patch}{extra}"


def detect_current_version(project_root: str) -> Tuple[str, int]:
    """
    Detect the current semantic version and build code from project files.

    Inspects `androidApp/build.gradle.kts` and `iosApp/Configuration/Config.xcconfig`
    to determine the current version and build number.

    :param project_root: The root directory of the project.
    :type project_root: str
    :return: A tuple of (current_version_name, current_build_code).
    :rtype: tuple[str, int]
    :raises RuntimeError: If version or build code cannot be detected.
    """
    android_build = os.path.join(project_root, "androidApp", "build.gradle.kts")
    ios_config = os.path.join(
        project_root, "iosApp", "Configuration", "Config.xcconfig"
    )

    version_name: Optional[str] = None
    version_code: Optional[int] = None

    if os.path.exists(android_build):
        with open(android_build, "r", encoding="utf-8") as f:
            content = f.read()
        vname_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
        vcode_match = re.search(r"versionCode\s*=\s*(\d+)", content)
        if vname_match:
            version_name = vname_match.group(1)
        if vcode_match:
            version_code = int(vcode_match.group(1))

    if os.path.exists(ios_config):
        with open(ios_config, "r", encoding="utf-8") as f:
            content = f.read()
        if not version_name:
            m_ver = re.search(r"MARKETING_VERSION\s*=\s*([^\r\n]+)", content)
            if m_ver:
                version_name = m_ver.group(1).strip()
        if version_code is None:
            c_ver = re.search(r"CURRENT_PROJECT_VERSION\s*=\s*(\d+)", content)
            if c_ver:
                version_code = int(c_ver.group(1))

    if version_name is None:
        raise RuntimeError("Failed to detect current application version string.")
    if version_code is None:
        raise RuntimeError(
            "Failed to detect current application build code/versionCode."
        )

    return version_name, version_code


def update_android_build(
    file_path: str, new_version: str, new_code: int, dry_run: bool = False
) -> bool:
    """
    Update versionCode and versionName in `androidApp/build.gradle.kts`.

    :param file_path: Path to `androidApp/build.gradle.kts`.
    :type file_path: str
    :param new_version: The new semantic version string.
    :type new_version: str
    :param new_code: The new integer versionCode.
    :type new_code: int
    :param dry_run: If True, do not write changes to disk.
    :type dry_run: bool
    :return: True if the file was modified or would be modified.
    :rtype: bool
    :raises RuntimeError: If expected version entries were not found in the file.
    """
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_content, count_code = re.subn(
        r"(versionCode\s*=\s*)\d+", rf"\g<1>{new_code}", content
    )
    new_content, count_name = re.subn(
        r'(versionName\s*=\s*)"[^"]+"', rf'\g<1>"{new_version}"', new_content
    )

    if count_code == 0 or count_name == 0:
        raise RuntimeError(
            f"Failed to match versionCode or versionName in {file_path} "
            f"(matched code: {count_code}, name: {count_name})"
        )

    if not dry_run and new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)

    return True


def update_ios_xcconfig(
    file_path: str, new_version: str, new_code: int, dry_run: bool = False
) -> bool:
    """
    Update CURRENT_PROJECT_VERSION and MARKETING_VERSION in `iosApp/Configuration/Config.xcconfig`.

    :param file_path: Path to `Config.xcconfig`.
    :type file_path: str
    :param new_version: The new semantic marketing version string.
    :type new_version: str
    :param new_code: The new integer build version.
    :type new_code: int
    :param dry_run: If True, do not write changes to disk.
    :type dry_run: bool
    :return: True if the file was modified or would be modified.
    :rtype: bool
    :raises RuntimeError: If expected version entries were not found in the file.
    """
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_content, count_code = re.subn(
        r"(CURRENT_PROJECT_VERSION\s*=\s*)\d+", rf"\g<1>{new_code}", content
    )
    new_content, count_name = re.subn(
        r"(MARKETING_VERSION\s*=\s*)[^\r\n]+", rf"\g<1>{new_version}", new_content
    )

    if count_code == 0 or count_name == 0:
        raise RuntimeError(
            f"Failed to match CURRENT_PROJECT_VERSION or MARKETING_VERSION in {file_path}"
        )

    if not dry_run and new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)

    return True


def update_ios_pbxproj(
    file_path: str, new_version: str, new_code: int, dry_run: bool = False
) -> bool:
    """
    Update CURRENT_PROJECT_VERSION and MARKETING_VERSION in `iosApp/iosApp.xcodeproj/project.pbxproj`.

    :param file_path: Path to `project.pbxproj`.
    :type file_path: str
    :param new_version: The new semantic marketing version string.
    :type new_version: str
    :param new_code: The new integer build version.
    :type new_code: int
    :param dry_run: If True, do not write changes to disk.
    :type dry_run: bool
    :return: True if the file was modified or would be modified.
    :rtype: bool
    :raises RuntimeError: If expected version entries were not found in the file.
    """
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_content, count_code = re.subn(
        r"(CURRENT_PROJECT_VERSION\s*=\s*)\d+(;)", rf"\g<1>{new_code}\2", content
    )
    new_content, count_name = re.subn(
        r"(MARKETING_VERSION\s*=\s*)[^;\r\n]+(;)", rf"\g<1>{new_version}\2", new_content
    )

    if count_code == 0 or count_name == 0:
        raise RuntimeError(
            f"Failed to match CURRENT_PROJECT_VERSION or MARKETING_VERSION in {file_path}"
        )

    if not dry_run and new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)

    return True


def update_jvm_package_version(
    file_path: str, new_version: str, dry_run: bool = False
) -> bool:
    """
    Update packageVersion in `chartCam/build.gradle.kts` for desktop JVM packaging.

    :param file_path: Path to `chartCam/build.gradle.kts`.
    :type file_path: str
    :param new_version: The new semantic version string.
    :type new_version: str
    :param dry_run: If True, do not write changes to disk.
    :type dry_run: bool
    :return: True if the file was modified or would be modified.
    :rtype: bool
    :raises RuntimeError: If packageVersion was not found in the file.
    """
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_content, count = re.subn(
        r'(packageVersion\s*=\s*)"[^"]+"', rf'\g<1>"{new_version}"', content
    )

    if count == 0:
        raise RuntimeError(f"Failed to match packageVersion in {file_path}")

    if not dry_run and new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)

    return True


def update_about_version(
    file_path: str, new_version: str, dry_run: bool = False
) -> bool:
    """
    Update the version string displayed in the About dialog in `PatientListScreen.kt`.

    :param file_path: Path to `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/ui/PatientListScreen.kt`.
    :type file_path: str
    :param new_version: The new semantic version string.
    :type new_version: str
    :param dry_run: If True, do not write changes to disk.
    :type dry_run: bool
    :return: True if the file was modified or would be modified.
    :rtype: bool
    :raises RuntimeError: If the version_text format call was not found in the file.
    """
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = r'stringResource\(Res\.string\.version_text,\s*"[^"]*?(\s*—\s*https://healthplatform\.io)"\)'
    new_content, count = re.subn(
        pattern,
        rf'stringResource(Res.string.version_text, "{new_version}\g<1>")',
        content,
    )

    if count == 0:
        raise RuntimeError(
            f"Failed to match about dialog version pattern in {file_path}"
        )

    if not dry_run and new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)

    return True


def bump_all_components(
    project_root: str,
    new_version: Optional[str] = None,
    new_code: Optional[int] = None,
    dry_run: bool = False,
) -> Dict[str, str]:
    """
    Bump the version and build number across Android, iOS, JVM desktop, and About screen.

    If new_version or new_code are not explicitly provided, the current version is detected
    and incremented by one patch version and one build code integer.

    :param project_root: Root directory of the repository.
    :type project_root: str
    :param new_version: Explicit target version string, or None to auto-bump patch.
    :type new_version: Optional[str]
    :param new_code: Explicit target build code integer, or None to auto-increment.
    :type new_code: Optional[int]
    :param dry_run: If True, simulate changes without writing to disk.
    :type dry_run: bool
    :return: Dictionary summary of the bump actions and target version numbers.
    :rtype: dict[str, str]
    """
    curr_version, curr_code = detect_current_version(project_root)

    target_version = new_version if new_version else bump_semver_patch(curr_version)
    target_code = new_code if new_code is not None else curr_code + 1

    android_build = os.path.join(project_root, "androidApp", "build.gradle.kts")
    ios_xcconfig = os.path.join(
        project_root, "iosApp", "Configuration", "Config.xcconfig"
    )
    ios_pbxproj = os.path.join(
        project_root, "iosApp", "iosApp.xcodeproj", "project.pbxproj"
    )
    jvm_build = os.path.join(project_root, "chartCam", "build.gradle.kts")
    about_screen = os.path.join(
        project_root,
        "chartCam",
        "src",
        "commonMain",
        "kotlin",
        "io",
        "healthplatform",
        "chartcam",
        "ui",
        "PatientListScreen.kt",
    )

    update_android_build(android_build, target_version, target_code, dry_run=dry_run)
    update_ios_xcconfig(ios_xcconfig, target_version, target_code, dry_run=dry_run)
    update_ios_pbxproj(ios_pbxproj, target_version, target_code, dry_run=dry_run)
    update_jvm_package_version(jvm_build, target_version, dry_run=dry_run)
    update_about_version(about_screen, target_version, dry_run=dry_run)

    return {
        "previous_version": curr_version,
        "new_version": target_version,
        "previous_build_code": str(curr_code),
        "new_build_code": str(target_code),
        "dry_run": str(dry_run),
    }


def main() -> None:
    """
    Parse command-line arguments and execute the patch bump workflow.
    """
    parser = argparse.ArgumentParser(
        description="Bump patch version and build code across Android, iOS, JVM, and About dialog."
    )
    parser.add_argument(
        "--patch",
        action="store_true",
        default=True,
        help="Bump the patch version by 1 (default behaviour).",
    )
    parser.add_argument(
        "--version",
        dest="custom_version",
        type=str,
        default=None,
        help="Specify an explicit version string instead of auto-bumping.",
    )
    parser.add_argument(
        "--build-code",
        dest="custom_build_code",
        type=int,
        default=None,
        help="Specify an explicit integer build code instead of auto-incrementing.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate the version bump without modifying files.",
    )
    parser.add_argument(
        "--project-root",
        type=str,
        default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        help="Path to project root directory.",
    )

    args = parser.parse_args()

    try:
        summary = bump_all_components(
            project_root=args.project_root,
            new_version=args.custom_version,
            new_code=args.custom_build_code,
            dry_run=args.dry_run,
        )

        mode_str = " (DRY RUN)" if args.dry_run else ""
        print(f"Version bump succeeded{mode_str}:")
        print(
            f"  Version:     {summary['previous_version']} -> {summary['new_version']}"
        )
        print(
            f"  Build Code:  {summary['previous_build_code']} -> {summary['new_build_code']}"
        )
        print("  Updated files:")
        print("    - androidApp/build.gradle.kts (versionCode, versionName)")
        print(
            "    - iosApp/Configuration/Config.xcconfig (CURRENT_PROJECT_VERSION, MARKETING_VERSION)"
        )
        print(
            "    - iosApp/iosApp.xcodeproj/project.pbxproj (CURRENT_PROJECT_VERSION, MARKETING_VERSION)"
        )
        print("    - chartCam/build.gradle.kts (packageVersion)")
        print(
            "    - chartCam/src/commonMain/kotlin/.../PatientListScreen.kt (About version)"
        )
    except Exception as exc:
        print(f"Error bumping version: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
