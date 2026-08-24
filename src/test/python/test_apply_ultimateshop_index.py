import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[3] / "tools" / "apply_ultimateshop_index.py"
SPEC = importlib.util.spec_from_file_location("apply_index", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class UltimateShopIndexTest(unittest.TestCase):
    def test_only_price_amounts_are_changed(self):
        source = """items:
  A:
    price-mode: CLASSIC_ALL
    products:
      1:
        material: WHEAT
        amount: 16
    buy-prices:
      1:
        amount: '32'
    sell-prices:
      1:
        amount: 20
"""
        result, changed = MODULE.transform(source)
        self.assertEqual(2, changed)
        self.assertIn("price-mode: ALL", result)
        self.assertIn("amount: 16", result)
        self.assertIn("amount: '32*%drakesmarket_buy_factor%'", result)
        self.assertIn("amount: '20*%drakesmarket_sell_factor%'", result)


    def test_transform_is_idempotent(self):
        source = """buy-prices:
  1:
    amount: '10*%drakesmarket_buy_factor%'
"""
        result, changed = MODULE.transform(source)
        self.assertEqual(0, changed)
        self.assertEqual(source, result)


if __name__ == "__main__":
    unittest.main()
