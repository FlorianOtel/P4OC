"""Typed CSV ingestion for shipment data."""

from __future__ import annotations

import csv
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import TextIO


class DataError(ValueError):
    """A source row could not be converted into a shipment."""


@dataclass(frozen=True, slots=True)
class Shipment:
    shipment_id: str
    carrier: str
    origin: str
    destination: str
    shipped_date: date
    promised_date: date
    delivered_date: date | None
    revenue_usd: Decimal
    delay_hours: Decimal

    @property
    def route(self) -> str:
        return f"{self.origin}-{self.destination}"


def load_shipments(path: str | Path) -> list[Shipment]:
    with Path(path).open(newline="", encoding="utf-8") as source:
        return read_shipments(source)


def read_shipments(source: TextIO) -> list[Shipment]:
    reader = csv.DictReader(source)
    required = {
        "shipment_id", "carrier", "origin", "destination", "shipped_date",
        "promised_date", "delivered_date", "revenue_usd", "delay_hours",
    }
    missing = sorted(required.difference(reader.fieldnames or ()))
    if missing:
        raise DataError(f"missing columns: {', '.join(missing)}")

    shipments: list[Shipment] = []
    for row_number, row in enumerate(reader, start=2):
        try:
            shipment = _parse_row(row)
        except (KeyError, InvalidOperation, ValueError) as error:
            raise DataError(f"row {row_number}: {error}") from error
        shipments.append(shipment)
    return shipments


def _parse_row(row: dict[str, str | None]) -> Shipment:
    values = {key: (value or "").strip() for key, value in row.items()}
    for field in ("shipment_id", "carrier", "origin", "destination"):
        if not values[field]:
            raise ValueError(f"{field} is required")

    shipped = date.fromisoformat(values["shipped_date"])
    promised = date.fromisoformat(values["promised_date"])
    delivered = date.fromisoformat(values["delivered_date"]) if values["delivered_date"] else None
    revenue = Decimal(values["revenue_usd"])
    delay = Decimal(values["delay_hours"])
    if promised < shipped:
        raise ValueError("promised_date cannot precede shipped_date")
    if revenue < 0:
        raise ValueError("revenue_usd cannot be negative")
    return Shipment(
        shipment_id=values["shipment_id"],
        carrier=values["carrier"],
        origin=values["origin"].upper(),
        destination=values["destination"].upper(),
        shipped_date=shipped,
        promised_date=promised,
        delivered_date=delivered,
        revenue_usd=revenue,
        delay_hours=delay,
    )
