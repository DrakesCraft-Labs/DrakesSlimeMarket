package cl.drakescraft.slimemarket;

import org.bukkit.inventory.ItemStack;

record CatalogEntry(String id, String addon, String displayName, ItemStack prototype, double basePrice) {

    /** Crea una copia entregable sin compartir metadatos mutables con el registro de Slimefun. */
    ItemStack createItem(int amount) {
        final ItemStack item = prototype.clone();
        item.setAmount(amount);
        return item;
    }

    int maxStackSize() {
        return prototype.getMaxStackSize();
    }
}
