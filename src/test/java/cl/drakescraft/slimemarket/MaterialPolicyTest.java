package cl.drakescraft.slimemarket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialPolicyTest {
    private final MaterialPolicy policy = new MaterialPolicy(
        List.of("MANUAL_CATALYST"),
        List.of("DUST", "INGOT", "PLATE"),
        List.of("InfinityExpansion", "Supreme"),
        List.of("INFINITY", "SUPREME"),
        List.of("MACHINE", "GENERATOR", "CHEAT", "ARMOR"),
        List.of("Machine", "Generator", "Weapon", "Tool")
    );

    @Test
    void acceptsRealMaterialFamilies() {
        assertTrue(policy.isAllowed("MAGNESIUM_DUST", "Slimefun", "SlimefunItem", "GUNPOWDER", false));
        assertTrue(policy.isAllowed("MANUAL_CATALYST", "Galactifun", "SlimefunItem", "PRISMARINE_SHARD", false));
    }

    @Test
    void blockedAddonWinsEvenForExplicitEntry() {
        assertFalse(policy.isAllowed("VOID_INGOT", "InfinityExpansion", "SlimefunItem", "IRON_INGOT", true));
    }

    @Test
    void rejectsMachinesAndEquipment() {
        assertFalse(policy.isAllowed("COPPER_DUST_MACHINE", "DynaTech", "ElectricMachine", "FURNACE", true));
        assertFalse(policy.isAllowed("DIVINE_INGOT", "Relics", "SlimefunItem", "NETHERITE_SWORD", true));
    }
}
