"""
This module provides a script to verify internationalization (i18n) completeness.
It checks that all string resources defined in the base `strings.xml` file have
corresponding translations in the localized `strings.xml` files for supported locales.
"""

import xml.etree.ElementTree as ET
import os
import sys


def get_keys(filepath):
    """
    Extract the set of string resource names from an Android-style strings.xml file.

    This function parses the provided XML file and extracts the value of the `name`
    attribute for every `<string>` element. This set of names represents the available
    translation keys in that specific file.

    :param filepath: The file path to the `strings.xml` resource file.
    :type filepath: str
    :return: A set containing all the string resource keys defined in the file.
             Returns an empty set if the file does not exist.
    :rtype: set[str]
    """
    if not os.path.exists(filepath):
        return set()
    tree = ET.parse(filepath)
    root = tree.getroot()
    return set(child.get("name") for child in root if child.tag == "string")


base_dir = "chartCam/src/commonMain/composeResources/values"
base_keys = get_keys(os.path.join(base_dir, "strings.xml"))

locales = ["es", "ja"]
missing = False

for loc in locales:
    loc_keys = get_keys(os.path.join(base_dir + "-" + loc, "strings.xml"))
    missing_keys = base_keys - loc_keys
    if missing_keys:
        print(f"Missing in {loc}: {missing_keys}")
        missing = True

if missing:
    sys.exit(1)
else:
    print("All translations are complete!")
