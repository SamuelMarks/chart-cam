import xml.etree.ElementTree as ET
import os
import sys

def get_keys(filepath):
    if not os.path.exists(filepath):
        return set()
    tree = ET.parse(filepath)
    root = tree.getroot()
    return set(child.get('name') for child in root if child.tag == 'string')

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
