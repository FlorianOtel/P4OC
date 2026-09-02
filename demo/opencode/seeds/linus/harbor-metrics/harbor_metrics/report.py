"""Aggregations over shipment records."""

from __future__ import annotations

from collections import Counter
from decimal import Decimal
from typing import Iterable, TypedDict

from .records import Shipment


class Summary(TypedDict):
    shipments: int
    delivered: int
    delayed: int
    revenue_usd: Decimal
    routes: dict[str, int]


def is_delayed(shipment: Shipment) -> bool:
    # TODO: use promised/delivered dates; a positive carrier delay is not always late.
    return shipment.delivered_date is not None and shipment.delay_hours > 0


def delayed_shipments(shipments: Iterable[Shipment], minimum_hours: Decimal = Decimal("0")) -> list[Shipment]:
    return sorted(
        (
            shipment
            for shipment in shipments
            if is_delayed(shipment) and shipment.delay_hours >= minimum_hours
        ),
        key=lambda shipment: (-shipment.delay_hours, shipment.shipment_id),
    )


def summarize(shipments: Iterable[Shipment]) -> Summary:
    records = list(shipments)
    routes = Counter(shipment.route for shipment in records)
    return {
        "shipments": len(records),
        "delivered": sum(shipment.delivered_date is not None for shipment in records),
        "delayed": sum(is_delayed(shipment) for shipment in records),
        "revenue_usd": sum((shipment.revenue_usd for shipment in records), start=Decimal("0")),
        "routes": dict(sorted(routes.items())),
    }
