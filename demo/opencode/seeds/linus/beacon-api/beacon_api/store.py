"""In-memory incident storage and domain rules."""

from __future__ import annotations

import json
from collections import Counter
from copy import deepcopy
from pathlib import Path
from threading import Lock
from typing import Any

VALID_STATUSES = frozenset({"open", "monitoring", "resolved"})
VALID_SEVERITIES = frozenset({"minor", "major", "critical"})


class IncidentStore:
    def __init__(self, incidents: list[dict[str, Any]]) -> None:
        self._incidents = deepcopy(incidents)
        self._lock = Lock()

    @classmethod
    def from_json(cls, path: Path) -> "IncidentStore":
        with path.open(encoding="utf-8") as source:
            payload = json.load(source)
        if not isinstance(payload, list):
            raise ValueError("incident fixture must contain a JSON array")
        return cls(payload)

    def list(self, *, status: str | None = None, service: str | None = None) -> list[dict[str, Any]]:
        # TODO: status filtering is currently case-sensitive (see DEMO_TASKS.md).
        matches = self._incidents
        if status is not None:
            matches = [item for item in matches if item["status"] == status]
        if service is not None:
            matches = [item for item in matches if item["service"].casefold() == service.casefold()]
        return deepcopy(matches)

    def create(self, payload: dict[str, Any]) -> dict[str, Any]:
        required = ("title", "service", "severity")
        missing = [field for field in required if not str(payload.get(field, "")).strip()]
        if missing:
            raise ValueError(f"missing required fields: {', '.join(missing)}")
        severity = str(payload["severity"]).lower()
        if severity not in VALID_SEVERITIES:
            raise ValueError(f"severity must be one of: {', '.join(sorted(VALID_SEVERITIES))}")

        with self._lock:
            next_number = max((int(item["id"].split("-")[-1]) for item in self._incidents), default=0) + 1
            incident = {
                "id": f"inc-{next_number:04d}",
                "title": str(payload["title"]).strip(),
                "service": str(payload["service"]).strip(),
                "severity": severity,
                "status": "open",
            }
            self._incidents.append(incident)
        return deepcopy(incident)

    def summary(self) -> dict[str, Any]:
        by_status = Counter(item["status"] for item in self._incidents)
        return {"total": len(self._incidents), "by_status": dict(sorted(by_status.items()))}
