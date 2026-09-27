# scripts/gen-evaluation-expected.py
import json
from pathlib import Path

RULES = {
    "001": "SRS-DI-01-001",
    "002": "SRS-DI-02-001",
    "003": "SRS-DAO-01-001",
    "004": "SRS-DAO-02-001",
    "005": "SRS-DAO-03-001",
    "006": "SRS-DAO-04-001",
    "007": "SRS-WEB-01-001",
    "008": "SRS-WEB-02-001",
    "009": "SRS-WEB-04-001",
    "010": "SRS-CON-01-001",
    "011": "SRS-SEC-01-001",
    "012": "SRS-SEC-02-001",
    "013": "SRS-SEC-03-001",
    "014": "SRS-SEC-04-001",
    "015": "SRS-EXC-01-001",
    "016": "SRS-EXC-02-001",
    "017": "SRS-RES-01-001",
    "018": "SRS-OBS-01-001",
    "019": "SRS-MIG-01-001",
    "020": "SRS-I18N-01-001",
}

def main():
    out_dir = Path("tests/evaluation/expected")
    out_dir.mkdir(parents=True, exist_ok=True)

    for num, rule in RULES.items():
        case_id = f"DEFECT-{num}"
        payload = {
            "case_id": case_id,
            "type": "defect",
            "input": f"defects/{case_id}.java",
            "expected_rules": [rule],
        }
        (out_dir / f"{case_id}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    for num in range(1, 11):
        case_id = f"CLEAN-{num:03d}"
        payload = {
            "case_id": case_id,
            "type": "clean",
            "input": f"clean/{case_id}.java",
            "expected_rules": [],
        }
        (out_dir / f"{case_id}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    print(f"Generated {len(RULES)} defect + 10 clean expected JSONs")

if __name__ == "__main__":
    main()