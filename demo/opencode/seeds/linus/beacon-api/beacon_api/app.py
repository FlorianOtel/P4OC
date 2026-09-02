"""HTTP transport for the Beacon incident service."""

from __future__ import annotations

import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import parse_qs, urlparse

from .store import IncidentStore


def _handler_for(store: IncidentStore) -> type[BaseHTTPRequestHandler]:
    class BeaconHandler(BaseHTTPRequestHandler):
        server_version = "BeaconAPI/0.1"

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            request = urlparse(self.path)
            if request.path == "/health":
                self._send(HTTPStatus.OK, {"status": "ok"})
                return
            if request.path == "/api/v1/incidents":
                query = parse_qs(request.query)
                incidents = store.list(
                    status=_first(query, "status"),
                    service=_first(query, "service"),
                )
                self._send(HTTPStatus.OK, {"incidents": incidents, "count": len(incidents)})
                return
            if request.path == "/api/v1/summary":
                self._send(HTTPStatus.OK, store.summary())
                return
            self._error(HTTPStatus.NOT_FOUND, "not_found", "route not found")

        def do_POST(self) -> None:  # noqa: N802 - stdlib handler API
            if urlparse(self.path).path != "/api/v1/incidents":
                self._error(HTTPStatus.NOT_FOUND, "not_found", "route not found")
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                body = self.rfile.read(length)
                payload = json.loads(body)
                if not isinstance(payload, dict):
                    raise ValueError("request body must be a JSON object")
                incident = store.create(payload)
            except (json.JSONDecodeError, ValueError) as error:
                self._error(HTTPStatus.BAD_REQUEST, "invalid_request", str(error))
                return
            self._send(HTTPStatus.CREATED, incident)

        def log_message(self, format: str, *args: object) -> None:
            return

        def _error(self, status: HTTPStatus, code: str, message: str) -> None:
            self._send(status, {"error": {"code": code, "message": message}})

        def _send(self, status: HTTPStatus, payload: Any) -> None:
            encoded = json.dumps(payload, separators=(",", ":")).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)

    return BeaconHandler


def _first(query: dict[str, list[str]], name: str) -> str | None:
    values = query.get(name)
    return values[0] if values else None


def create_server(host: str, port: int, store: IncidentStore) -> ThreadingHTTPServer:
    return ThreadingHTTPServer((host, port), _handler_for(store))
