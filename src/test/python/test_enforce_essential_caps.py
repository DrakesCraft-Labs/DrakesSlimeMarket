import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[3] / "tools" / "enforce_essential_caps.py"
SPEC = importlib.util.spec_from_file_location("essential_caps", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class EssentialCapsTest(unittest.TestCase):
    def test_caps_bundle_without_touching_product_amount(self):
        source = """items:
  A:
    products:
      1:
        material: BLUE_WOOL
        amount: 16
    buy-prices:
      1:
        amount: '80*%drakesmarket_buy_factor%'
    sell-prices:
      1:
        amount: '32*%drakesmarket_sell_factor%'
"""
        result, changed = MODULE.transform(Path("wools.yml"), source)
        self.assertEqual(4, changed)
        self.assertIn("amount: 16", result)
        self.assertIn("amount: '32*%drakesmarket_buy_factor%'", result)
        self.assertIn("max-amount: 32", result)
        self.assertIn("amount: '4*%drakesmarket_sell_factor%'", result)
        self.assertIn("max-amount: 4", result)

    def test_reversible_copper_is_excluded(self):
        source = """items:
  A:
    products:
      1:
        material: COPPER_BLOCK
    buy-prices:
      1:
        amount: '600*%drakesmarket_buy_factor%'
"""
        result, changed = MODULE.transform(Path("blocks_121.yml"), source)
        self.assertEqual(0, changed)
        self.assertEqual(source, result)

    def test_second_pass_is_idempotent(self):
        source = """items:
  A:
    products:
      1:
        material: POPPY
    buy-prices:
      1:
        amount: '2*%drakesmarket_buy_factor%'
        max-amount: 2
"""
        result, changed = MODULE.transform(Path("flowers.yml"), source)
        self.assertEqual(0, changed)
        self.assertEqual(source, result)


if __name__ == "__main__":
    unittest.main()
