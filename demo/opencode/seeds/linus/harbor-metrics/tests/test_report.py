from __future__ import annotations

import unittest
from datetime import date
from decimal import Decimal

from harbor_metrics.records import Shipment
from harbor_metrics.report import delayed_shipments, summarize


def shipment(identifier: str, *, delay: str, revenue: str = "10.00", delivered: bool = True) -> Shipment:
    return Shipment(
        shipment_id=identifier,
        carrier="Swift",
        origin="SEA",
        destination="PDX",
        shipped_date=date(2026, 1, 1),
        promised_date=date(2026, 1, 3),
        delivered_date=date(2026, 1, 4) if delivered else None,
        revenue_usd=Decimal(revenue),
        delay_hours=Decimal(delay),
    )


class ReportTests(unittest.TestCase):
    def test_summary_aggregates_counts_revenue_and_routes(self) -> None:
        result = summarize([shipment("S-1", delay="4", revenue="12.40"), shipment("S-2", delay="0", revenue="7.60", delivered=False)])
        self.assertEqual(2, result["shipments"])
        self.assertEqual(1, result["delivered"])
        self.assertEqual(1, result["delayed"])
        self.assertEqual(Decimal("20.00"), result["revenue_usd"])
        self.assertEqual({"SEA-PDX": 2}, result["routes"])

    def test_delayed_rows_are_sorted_by_delay_then_id(self) -> None:
        rows = [shipment("S-2", delay="2"), shipment("S-3", delay="5"), shipment("S-1", delay="5")]
        self.assertEqual(["S-1", "S-3", "S-2"], [row.shipment_id for row in delayed_shipments(rows)])

    def test_minimum_delay_is_inclusive(self) -> None:
        rows = [shipment("S-1", delay="3.9"), shipment("S-2", delay="4")]
        self.assertEqual(["S-2"], [row.shipment_id for row in delayed_shipments(rows, Decimal("4"))])


if __name__ == "__main__":
    unittest.main()
