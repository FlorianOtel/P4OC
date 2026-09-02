from __future__ import annotations

import io
import unittest
from decimal import Decimal

from harbor_metrics.records import DataError, read_shipments

HEADER = "shipment_id,carrier,origin,destination,shipped_date,promised_date,delivered_date,revenue_usd,delay_hours\n"


class RecordTests(unittest.TestCase):
    def test_parses_types_and_normalizes_airport_codes(self) -> None:
        rows = read_shipments(io.StringIO(HEADER + "S-1,Swift,sea,pdx,2026-01-01,2026-01-03,2026-01-02,12.30,-1\n"))
        self.assertEqual("SEA-PDX", rows[0].route)
        self.assertEqual(Decimal("12.30"), rows[0].revenue_usd)

    def test_allows_undelivered_shipment(self) -> None:
        rows = read_shipments(io.StringIO(HEADER + "S-2,Swift,SEA,PDX,2026-01-01,2026-01-03,,9.00,0\n"))
        self.assertIsNone(rows[0].delivered_date)

    def test_reports_bad_row_number(self) -> None:
        with self.assertRaisesRegex(DataError, "row 2: Invalid isoformat"):
            read_shipments(io.StringIO(HEADER + "S-3,Swift,SEA,PDX,not-a-date,2026-01-03,,9.00,0\n"))

    def test_reports_missing_columns(self) -> None:
        with self.assertRaisesRegex(DataError, "missing columns"):
            read_shipments(io.StringIO("shipment_id,carrier\nS-1,Swift\n"))


if __name__ == "__main__":
    unittest.main()
