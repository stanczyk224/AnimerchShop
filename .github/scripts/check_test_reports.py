"""Summarize Surefire XML and fail if PostgreSQL coverage did not execute."""

import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def main():
    report_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("target/surefire-reports")
    required = {
        "com.shop.animerch.CheckoutDatabaseTests",
        "com.shop.animerch.WishlistDatabaseTests",
        "com.shop.animerch.ProductRatingDatabaseTests",
        "com.shop.animerch.ProductFilterDatabaseTests",
        "com.shop.animerch.AnimerchApplicationTests",
    }
    seen = set()
    problems = []
    totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
    rows = []
    for report in sorted(report_dir.glob("TEST-*.xml")):
        try:
            suite = ET.parse(report).getroot()
            name = suite.attrib["name"]
            counts = {key: int(suite.get(key, "0")) for key in totals}
        except (ET.ParseError, OSError, KeyError, ValueError) as error:
            problems.append(f"Cannot read {report.name}: {error}")
            continue
        seen.add(name)
        for key, value in counts.items():
            totals[key] += value
        rows.append(f"| {name} | " + " | ".join(str(counts[key]) for key in totals) + " |")
        if name in required and (counts["tests"] < 1 or counts["skipped"] > 0):
            problems.append(f"Required database suite did not fully execute: {name}")
    for name in sorted(required - seen):
        problems.append(f"Missing required database suite: {name}")
    if totals["failures"] or totals["errors"]:
        problems.append("Test failures or errors were reported")

    summary = "\n".join([
        "## Test results", "",
        "| Suite | Tests | Failures | Errors | Skipped |",
        "| --- | ---: | ---: | ---: | ---: |",
        *rows,
        "| **Total** | " + " | ".join(str(value) for value in totals.values()) + " |",
        "", *[f"- {problem}" for problem in problems], "",
    ])
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as output:
            output.write(summary)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
