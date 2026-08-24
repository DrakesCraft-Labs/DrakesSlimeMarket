#!/usr/bin/env python3
"""Añade el índice global a precios UltimateShop sin tocar productos ni cantidades."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

SECTION_RE = re.compile(r"^(?P<indent>\s*)(?P<section>buy-prices|sell-prices):\s*$")
AMOUNT_RE = re.compile(r"^(?P<indent>\s*)amount:\s*(?P<quote>['\"]?)(?P<value>\d+(?:\.\d+)?)(?P=quote)\s*$")


def transform(text: str) -> tuple[str, int]:
    """Transforma solamente montos numéricos dentro de buy/sell-prices."""
    lines = text.splitlines(keepends=True)
    active_section: str | None = None
    section_indent = -1
    changed = 0

    for index, original in enumerate(lines):
        line = original.rstrip("\r\n")
        newline = original[len(line):]
        section = SECTION_RE.match(line)
        if section:
            active_section = section.group("section")
            section_indent = len(section.group("indent"))
            continue

        indent = len(line) - len(line.lstrip())
        if active_section and line.strip() and indent <= section_indent:
            active_section = None
        if not active_section or "%drakesmarket_" in line:
            continue

        amount = AMOUNT_RE.match(line)
        if amount and indent > section_indent:
            factor = "buy_factor" if active_section == "buy-prices" else "sell_factor"
            lines[index] = (
                f"{amount.group('indent')}amount: '{amount.group('value')}*%drakesmarket_{factor}%'" + newline
            )
            changed += 1

    if changed:
        lines = [
            line.replace("price-mode: CLASSIC_ALL", "price-mode: ALL")
            for line in lines
        ]
    return "".join(lines), changed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+", type=Path)
    parser.add_argument("--check", action="store_true", help="No escribe; falla si quedaría algo por cambiar.")
    args = parser.parse_args()
    total = 0

    try:
        for path in args.paths:
            source = path.read_text(encoding="utf-8")
            result, changed = transform(source)
            total += changed
            if changed and not args.check:
                path.write_text(result, encoding="utf-8")
            print(f"[INFO] {path}: {changed} precios transformados")
    except (OSError, UnicodeError) as exception:
        print(f"[ERROR] No se pudo procesar la configuración: {exception}")
        return 2

    if args.check and total:
        print(f"[ERROR] Quedan {total} precios sin integrar")
        return 1
    print(f"[SUCCESS] Total: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
