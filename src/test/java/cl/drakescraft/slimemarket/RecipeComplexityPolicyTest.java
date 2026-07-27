package cl.drakescraft.slimemarket;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeComplexityPolicyTest {
    @Test
    void rejectsRecipesThatJumpTooManyCustomComponents() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("catalog.recipe-safety.enabled", true);
        config.set("catalog.recipe-safety.maximum-input-slots", 9);
        config.set("catalog.recipe-safety.maximum-slimefun-inputs", 2);
        RecipeComplexityPolicy policy = new RecipeComplexityPolicy(config);

        assertTrue(policy.isAllowed(9, 2));
        assertFalse(policy.isAllowed(9, 3));
    }
}
