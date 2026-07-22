package cl.drakescraft.slimemarket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialPolicyTest {
    private final MaterialPolicy policy = new MaterialPolicy(
        List.of("MANUAL_CATALYST"),
        List.of("DUST", "INGOT", "PLATE"),
        List.of("Slimefun", "Cultivation"),
        List.of("InfinityExpansion", "Supreme"),
        List.of("INFINITY", "SUPREME"),
        List.of("MACHINE", "GENERATOR", "CHEAT", "ARMOR"),
        List.of("Machine", "Generator", "Weapon", "Tool"),
        List.of("CHEST", "TABLE")
    );

    @Test
    void acceptsRealMaterialFamilies() {
        assertTrue(policy.isAllowed("MAGNESIUM_DUST", "Slimefun", "SlimefunItem", "GUNPOWDER", false));
        assertTrue(policy.isAllowed("MANUAL_CATALYST", "Galactifun", "SlimefunItem", "PRISMARINE_SHARD", true));
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

    @Test
    void rejectsUncuratedAddonsAndStorageMaterials() {
        assertFalse(policy.isAllowed("ENDER_INGOT", "ColoredEnderChests", "SlimefunItem", "IRON_INGOT", false));
        assertFalse(policy.isAllowed("BASIC_INGOT", "Slimefun", "SlimefunItem", "ENDER_CHEST", false));
    }

    @Test
    void wildcardInspectsEveryAddonWithoutBypassingRiskRules() {
        MaterialPolicy universal = new MaterialPolicy(
            List.of(),
            List.of("DUST", "INGOT", "PLATE"),
            List.of("*"),
            List.of("InfinityExpansion", "Supreme", "Networks"),
            List.of("INFINITY", "SUPREME"),
            List.of("MACHINE", "GENERATOR", "STORAGE"),
            List.of("Machine", "Generator", "Storage"),
            List.of("CHEST", "TABLE")
        );

        assertTrue(universal.isAllowed("STAINLESS_STEEL_INGOT", "DynaTech", "SlimefunItem", "IRON_INGOT", false));
        assertTrue(universal.isAllowed("MOON_DUST", "Galactifun2", "SlimefunItem", "GLOWSTONE_DUST", false));
        assertFalse(universal.isAllowed("SUPREME_INGOT", "Supreme", "SlimefunItem", "NETHERITE_INGOT", false));
        assertFalse(universal.isAllowed("NETWORK_STORAGE_INGOT", "NetworksV6-Drake", "StorageItem", "IRON_INGOT", false));
    }

    @Test
    void highTierNamesRemainExcludedFromUniversalDiscovery() {
        MaterialPolicy universal = new MaterialPolicy(
            List.of(), List.of("DUST", "INGOT", "PLATE"), List.of("*"), List.of(), List.of(),
            List.of("TITANIUM", "IRIDIUM", "VOID", "INFINITE", "ULTIMATE", "DRAGON"),
            List.of("Machine", "Generator"), List.of("CHEST", "TABLE")
        );

        assertFalse(universal.isAllowed("TITANIUM_INGOT", "FoxyMachines", "SlimefunItem", "IRON_INGOT", false));
        assertFalse(universal.isAllowed("VOID_DUST", "Galaxyfun", "SlimefunItem", "GLOWSTONE_DUST", false));
        assertTrue(universal.isAllowed("ZINC_DUST", "DynaTech", "SlimefunItem", "GUNPOWDER", false));
    }
}
