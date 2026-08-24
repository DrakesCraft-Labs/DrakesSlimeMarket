#!/usr/bin/env python3
"""Impone topes por unidad a materiales comunes sin reformatear UltimateShop."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

import yaml

BUY_CAP = 2.0
SELL_CAP = 0.25
CHEAP_SHOPS = {"flowers", "wools", "farming", "bambu_cerezo", "logs", "blocks"}
BLOCKS_121_EXCLUDED = {
    "BREEZE_ROD", "WIND_CHARGE", "CREAKING_HEART", "RESIN_CLUMP",
    "COPPER_GRATE", "CHISELED_COPPER", "CUT_COPPER", "OXIDIZED_CUT_COPPER",
    "COPPER_BULB", "COPPER_DOOR", "COPPER_TRAPDOOR", "WAXED_COPPER_BLOCK",
    "WAXED_EXPOSED_COPPER", "WAXED_WEATHERED_COPPER", "WAXED_OXIDIZED_COPPER",
}
ITEM_RE = re.compile(r"^  (?P<id>[^\s:#]+):\s*$")
SECTION_RE = re.compile(r"^    (?P<section>buy-prices|sell-prices):\s*$")
AMOUNT_RE = re.compile(r"^(?P<indent>\s*)amount:\s*['\"]?(?P<base>\d+(?:\.\d+)?)(?P<suffix>[^'\"\r\n]*)['\"]?\s*$")
MAX_RE = re.compile(r"^(?P<indent>\s*)max-amount:\s*.+$")


def capped_material(shop: str, material: str) -> bool:
    """Decide si el objeto es decorativo/común y no un recurso reversible raro."""
    if shop in CHEAP_SHOPS:
        return True
    if shop == "blocks_121":
        return material not in BLOCKS_121_EXCLUDED and "COPPER" not in material
    if shop == "copper_tuff":
        return "TUFF" in material
    return False


def number(value: float) -> str:
    return str(int(value)) if value.is_integer() else f"{value:.2f}".rstrip("0").rstrip(".")


def transform(path: Path, text: str) -> tuple[str, int]:
    """Reduce bases y añade max-amount conservando comentarios y formato."""
    document = yaml.safe_load(text) or {}
    shop = path.stem
    policies: dict[str, tuple[float, float]] = {}
    for item_id, item in (document.get("items") or {}).items():
        product = next(iter((item.get("products") or {}).values()), {})
        material = str(product.get("material", item_id)).upper()
        quantity = float(product.get("amount", 1) or 1)
        if capped_material(shop, material):
            policies[str(item_id)] = (BUY_CAP * quantity, SELL_CAP * quantity)

    lines = text.splitlines(keepends=True)
    current_item: str | None = None
    current_section: str | None = None
    changed = 0
    index = 0
    while index < len(lines):
        raw = lines[index]
        line = raw.rstrip("\r\n")
        newline = raw[len(line):]
        item_match = ITEM_RE.match(line)
        if item_match:
            current_item = item_match.group("id")
            current_section = None
        section_match = SECTION_RE.match(line)
        if section_match:
            current_section = section_match.group("section")
        if current_item not in policies or current_section not in {"buy-prices", "sell-prices"}:
            index += 1
            continue

        amount = AMOUNT_RE.match(line)
        if not amount or len(amount.group("indent")) < 8:
            index += 1
            continue
        cap = policies[current_item][0 if current_section == "buy-prices" else 1]
        base = float(amount.group("base"))
        suffix = amount.group("suffix")
        capped_base = min(base, cap)
        replacement = f"{amount.group('indent')}amount: '{number(capped_base)}{suffix}'{newline}"
        if replacement != raw:
            lines[index] = replacement
            changed += 1

        max_line = f"{amount.group('indent')}max-amount: {number(cap)}{newline}"
        if index + 1 < len(lines) and MAX_RE.match(lines[index + 1].rstrip("\r\n")):
            if lines[index + 1] != max_line:
                lines[index + 1] = max_line
                changed += 1
        else:
            lines.insert(index + 1, max_line)
            changed += 1
        index += 2
    return "".join(lines), changed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    total = 0
    try:
        for path in args.paths:
            source = path.read_text(encoding="utf-8")
            result, changed = transform(path, source)
            total += changed
            if changed and not args.check:
                path.write_text(result, encoding="utf-8")
            print(f"[INFO] {path}: {changed} ajustes")
    except (OSError, UnicodeError, yaml.YAMLError, ValueError) as exception:
        print(f"[ERROR] No se pudo aplicar la política: {exception}")
        return 2
    if args.check and total:
        print(f"[ERROR] Quedan {total} ajustes pendientes")
        return 1
    print(f"[SUCCESS] Total: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
