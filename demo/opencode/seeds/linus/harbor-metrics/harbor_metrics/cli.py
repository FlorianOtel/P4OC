"""Command-line interface for shipment reports."""

from __future__ import annotations

import argparse
import sys
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Sequence

from .records import DataError, load_shipments
from .report import delayed_shipments, summarize


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="harbor-metrics", description="Analyze shipment CSV data")
    commands = parser.add_subparsers(dest="command", required=True)

    summary = commands.add_parser("summarize", help="print overall metrics")
    summary.add_argument("csv", type=Path)

    delayed = commands.add_parser("delayed", help="list delayed shipments")
    delayed.add_argument("csv", type=Path)
    delayed.add_argument("--min-hours", default="0", type=_decimal)
    return parser


def _decimal(value: str) -> Decimal:
    try:
        parsed = Decimal(value)
    except InvalidOperation as error:
        raise argparse.ArgumentTypeError("must be a decimal number") from error
    if parsed < 0:
        raise argparse.ArgumentTypeError("must not be negative")
    return parsed


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        shipments = load_shipments(args.csv)
    except (DataError, OSError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2

    if args.command == "summarize":
        result = summarize(shipments)
        print(f"Shipments: {result['shipments']}")
        print(f"Delivered: {result['delivered']}")
        print(f"Delayed: {result['delayed']}")
        print(f"Revenue (USD): {result['revenue_usd']:.2f}")
        print("Routes:")
        for route, count in result["routes"].items():
            print(f"  {route}: {count}")
        return 0

    print("shipment_id\tcarrier\troute\tdelay_hours")
    for shipment in delayed_shipments(shipments, args.min_hours):
        print(f"{shipment.shipment_id}\t{shipment.carrier}\t{shipment.route}\t{shipment.delay_hours}")
    return 0
