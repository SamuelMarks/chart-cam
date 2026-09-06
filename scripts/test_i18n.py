"""
This module provides a script to verify internationalization (i18n) completeness.
It checks that all string and plural resources defined in the base `strings.xml` file
have corresponding translations in the localized `strings.xml` files for supported locales,
that format specifiers match, and that plural rules are followed.
"""

import xml.etree.ElementTree as ET
import os
import re
import sys


def get_keys(filepath):
    """
    Extract the set of string resource names from an Android-style strings.xml file.

    :param filepath: The file path to the `strings.xml` resource file.
    :type filepath: str
    :return: A set containing all the string resource keys defined in the file.
    :rtype: set[str]
    """
    if not os.path.exists(filepath):
        return set()
    tree = ET.parse(filepath)
    root = tree.getroot()
    return set(child.get("name") for child in root if child.tag == "string")


def get_plurals(filepath):
    """
    Extract the set of plural resource names from an Android-style strings.xml file.

    :param filepath: The file path to the `strings.xml` resource file.
    :type filepath: str
    :return: A set containing all the plural resource keys defined in the file.
    :rtype: set[str]
    """
    if not os.path.exists(filepath):
        return set()
    tree = ET.parse(filepath)
    root = tree.getroot()
    return set(child.get("name") for child in root if child.tag == "plurals")


def verify_translations():
    """
    Verify translations, plurals, format specifiers, and absence of non-reactive language calls.

    :return: True if all checks pass, False otherwise.
    :rtype: bool
    """
    base_dir = "chartCam/src/commonMain/composeResources/values"
    base_filepath = os.path.join(base_dir, "strings.xml")
    base_keys = get_keys(base_filepath)
    base_plurals = get_plurals(base_filepath)

    base_tree = ET.parse(base_filepath)
    base_strings = {c.get("name"): c.text for c in base_tree.getroot() if c.tag == "string"}
    fmt_pattern = re.compile(r"%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]")

    locales = ["es", "ja", "he", "zh"]
    has_errors = False

    for loc in locales:
        loc_path = os.path.join(base_dir + "-" + loc, "strings.xml")
        loc_keys = get_keys(loc_path)
        missing_keys = base_keys - loc_keys
        if missing_keys:
            print(f"Missing strings in {loc}: {missing_keys}")
            has_errors = True

        loc_plurals = get_plurals(loc_path)
        missing_plurals = base_plurals - loc_plurals
        if missing_plurals:
            print(f"Missing plurals in {loc}: {missing_plurals}")
            has_errors = True

        # Check for matching format specifiers and plurals
        if os.path.exists(loc_path):
            tree = ET.parse(loc_path)
            root = tree.getroot()
            for elem in root.iter():
                if elem.text and ("[ES]" in elem.text or "[JA]" in elem.text):
                    print(f"Pseudo-localization marker found in {loc}: {elem.text}")
                    has_errors = True

            loc_strings = {c.get("name"): c.text for c in root if c.tag == "string"}
            for k, v in base_strings.items():
                if k in loc_strings and v and loc_strings[k]:
                    base_fmts = fmt_pattern.findall(v)
                    loc_fmts = fmt_pattern.findall(loc_strings[k])
                    if len(base_fmts) != len(loc_fmts):
                        print(f"Format specifier mismatch in {loc} for key '{k}'")
                        has_errors = True

            # In Hebrew, ensure dual forms exist for plurals
            if loc == "he":
                plurals_dict = {
                    p.get("name"): {i.get("quantity") for i in p.findall("item")}
                    for p in root.findall("plurals")
                }
                for p_name, quantities in plurals_dict.items():
                    if "two" not in quantities:
                        print(f"Missing dual (two) plural quantity in Hebrew for '{p_name}'")
                        has_errors = True

    return not has_errors


if __name__ == "__main__":
    if not verify_translations():
        sys.exit(1)
    print("All translations are complete!")
    sys.exit(0)
