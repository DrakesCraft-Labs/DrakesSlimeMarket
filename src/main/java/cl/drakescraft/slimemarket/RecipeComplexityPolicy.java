package cl.drakescraft.slimemarket;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps the market on early materials instead of selling deep Slimefun crafting chains. */
final class RecipeComplexityPolicy {
    private final boolean enabled;
    private final int maximumInputSlots;
    private final int maximumSlimefunInputs;
    private final Set<String> exceptions;

    RecipeComplexityPolicy(FileConfiguration config) {
        enabled = config.getBoolean("catalog.recipe-safety.enabled", true);
        maximumInputSlots = Math.clamp(config.getInt("catalog.recipe-safety.maximum-input-slots", 9), 1, 9);
        maximumSlimefunInputs = Math.clamp(config.getInt("catalog.recipe-safety.maximum-slimefun-inputs", 2), 0, 9);
        exceptions = config.getStringList("catalog.recipe-safety.exceptions").stream()
            .map(id -> id.toUpperCase(java.util.Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    boolean isAllowed(SlimefunItem item) {
        if (!enabled || exceptions.contains(item.getId().toUpperCase(java.util.Locale.ROOT))) {
            return true;
        }

        int occupiedSlots = 0;
        int slimefunInputs = 0;
        for (ItemStack ingredient : item.getRecipe()) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }
            occupiedSlots++;
            if (SlimefunItem.getByItem(ingredient) != null) {
                slimefunInputs++;
            }
        }
        return isAllowed(occupiedSlots, slimefunInputs);
    }

    boolean isAllowed(int occupiedSlots, int slimefunInputs) {
        return !enabled || (occupiedSlots <= maximumInputSlots && slimefunInputs <= maximumSlimefunInputs);
    }
}
