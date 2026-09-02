from __future__ import annotations

import unittest

from beacon_api.store import IncidentStore


class IncidentStoreTests(unittest.TestCase):
    def setUp(self) -> None:
        self.store = IncidentStore(
            [
                {"id": "inc-0007", "title": "A", "service": "Billing", "severity": "major", "status": "open"},
                {"id": "inc-0009", "title": "B", "service": "search", "severity": "minor", "status": "resolved"},
            ]
        )

    def test_filters_service_without_case_sensitivity(self) -> None:
        self.assertEqual(["inc-0007"], [item["id"] for item in self.store.list(service="billing")])

    def test_returned_records_do_not_mutate_store(self) -> None:
        result = self.store.list()
        result[0]["title"] = "changed"
        self.assertEqual("A", self.store.list()[0]["title"])

    def test_create_assigns_next_id_and_open_status(self) -> None:
        created = self.store.create({"title": "New issue", "service": "api", "severity": "CRITICAL"})
        self.assertEqual("inc-0010", created["id"])
        self.assertEqual("open", created["status"])

    def test_create_rejects_missing_fields(self) -> None:
        with self.assertRaisesRegex(ValueError, "service, severity"):
            self.store.create({"title": "Incomplete"})


if __name__ == "__main__":
    unittest.main()
