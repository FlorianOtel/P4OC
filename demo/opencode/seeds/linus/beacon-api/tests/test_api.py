from __future__ import annotations

import json
import threading
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from beacon_api.app import create_server
from beacon_api.store import IncidentStore


class BeaconApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        store = IncidentStore(
            [{"id": "inc-0001", "title": "Queue lag", "service": "events", "severity": "major", "status": "open"}]
        )
        cls.server = create_server("127.0.0.1", 0, store)
        cls.base_url = f"http://127.0.0.1:{cls.server.server_port}"
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def request(self, path: str, *, data: dict[str, object] | None = None) -> tuple[int, dict[str, object]]:
        encoded = json.dumps(data).encode() if data is not None else None
        request = Request(self.base_url + path, data=encoded, headers={"Content-Type": "application/json"})
        with urlopen(request, timeout=2) as response:
            return response.status, json.load(response)

    def test_health(self) -> None:
        self.assertEqual((200, {"status": "ok"}), self.request("/health"))

    def test_list_can_filter_by_service(self) -> None:
        status, payload = self.request("/api/v1/incidents?service=EVENTS")
        self.assertEqual(200, status)
        self.assertEqual(1, payload["count"])

    def test_create_incident(self) -> None:
        status, payload = self.request(
            "/api/v1/incidents",
            data={"title": "Cache misses", "service": "catalog", "severity": "minor"},
        )
        self.assertEqual(201, status)
        self.assertEqual("open", payload["status"])

    def test_unknown_route_uses_structured_error(self) -> None:
        with self.assertRaises(HTTPError) as caught:
            self.request("/missing")
        with caught.exception as response:
            self.assertEqual(404, response.code)
            payload = json.load(response)
            self.assertEqual("not_found", payload["error"]["code"])


if __name__ == "__main__":
    unittest.main()
