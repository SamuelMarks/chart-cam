import os
import sys
import xml.etree.ElementTree as ET


def check_coverage(xml_path):
    if not os.path.exists(xml_path):
        print(f"Coverage report not found at {xml_path}")
        # To avoid failing the overall flow if kover hasn't run yet, but the instruction is to parse it
        return False

    tree = ET.parse(xml_path)
    root = tree.getroot()

    # The root element is usually <report>
    # Find the overall <counter> elements at the root level (or compute if not present)
    overall_counters = root.findall("./counter")

    passed = True
    for counter in overall_counters:
        ctype = counter.get("type")
        if ctype in ["INSTRUCTION", "BRANCH", "LINE", "METHOD"]:
            missed = int(counter.get("missed"))
            covered = int(counter.get("covered"))
            total = missed + covered
            if total > 0:
                percent = (covered / total) * 100
                print(f"{ctype} coverage: {percent:.2f}%")
                if percent < 100.0:
                    print(f"Error: {ctype} coverage is below 100.0% ({percent:.2f}%)")
                    passed = False
            else:
                print(f"{ctype} coverage: 100.0% (No items)")

    return passed


if __name__ == "__main__":
    # Typically kover generates something like build/reports/kover/report.xml
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    xml_path = os.path.join(project_root, "chartCam/build/reports/kover/report.xml")

    if os.path.exists(xml_path):
        if not check_coverage(xml_path):
            sys.exit(1)
        print("Test coverage is 100.0%")
        sys.exit(0)
    else:
        # Fallback to stub if no report is present so we don't break local runs unexpectedly
        # But wait, the instruction says "instead of stubbing 100.0% success."
        print(
            f"Could not find {xml_path}. Assuming tests did not run or coverage report missing."
        )
        sys.exit(1)
