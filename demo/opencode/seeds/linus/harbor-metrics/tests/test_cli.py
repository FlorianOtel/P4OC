from __future__ import annotations

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

from harbor_metrics.cli import main

FIXTURE = Path(__file__).resolve().parent.parent / "data" / "shipments.csv"


class CliTests(unittest.TestCase):
    def test_summary_command_prints_fixture_totals(self) -> None:
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            result = main(["summarize", str(FIXTURE)])
        self.assertEqual(0, result)
        self.assertIn("Shipments: 8", output.getvalue())
        self.assertIn("Revenue (USD): 14186.89", output.getvalue())

    def test_delayed_command_applies_threshold(self) -> None:
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            result = main(["delayed", str(FIXTURE), "--min-hours", "20"])
        self.assertEqual(0, result)
        self.assertIn("SHP-1008", output.getvalue())
        self.assertNotIn("SHP-1002", output.getvalue())

    def test_bad_csv_returns_nonzero_and_stderr_message(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.csv"
            path.write_text("shipment_id,carrier\nS-1,Swift\n", encoding="utf-8")
            errors = io.StringIO()
            with contextlib.redirect_stderr(errors):
                result = main(["summarize", str(path)])
        self.assertEqual(2, result)
        self.assertIn("missing columns", errors.getvalue())


if __name__ == "__main__":
    unittest.main()
