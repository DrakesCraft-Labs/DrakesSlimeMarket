package cl.drakescraft.slimemarket;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MarketMenu implements CommandExecutor, Listener {
    private static final String TITLE = ChatColor.DARK_GRAY + "Mercado de Materiales Slimefun";
    private final DrakesSlimeMarket plugin;
    private final Economy economy;
    private final Map<Integer, Offer> offers = new HashMap<>();

    MarketMenu(DrakesSlimeMarket plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        open(player);
        return true;
    }

    private void open(Player player) {
        offers.clear();
        final Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        int slot = 0;
        final ConfigurationSection entries = plugin.getConfig().getConfigurationSection("catalog.entries");
        if (entries == null) {
            player.openInventory(inventory);
            return;
        }

        for (String id : entries.getKeys(false)) {
            if (slot >= 54 || isBlocked(id)) continue;
            final SlimefunItem item = SlimefunItem.getById(id);
            final double price = plugin.getConfig().getDouble("catalog.entries." + id + ".price", -1);
            final int amount = plugin.getConfig().getInt("catalog.entries." + id + ".amount", 1);
            if (item == null || price < 0 || amount < 1 || amount > item.getItem().getMaxStackSize()) continue;
            final ItemStack display = new ItemStack(item.getItem());
            display.setAmount(amount);
            final ItemMeta meta = display.getItemMeta();
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Material aprobado por DrakesCraft");
            lore.add(ChatColor.GOLD + "Precio: " + economy.format(price));
            lore.add(ChatColor.YELLOW + "Click para comprar " + amount);
            meta.setLore(lore);
            display.setItemMeta(meta);
            inventory.setItem(slot, display);
            offers.put(slot++, new Offer(id, price, amount));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        final Offer offer = offers.get(event.getRawSlot());
        if (offer == null || event.getRawSlot() >= event.getInventory().getSize()) return;
        final SlimefunItem item = SlimefunItem.getById(offer.id());
        if (item == null || isBlocked(offer.id()) || !economy.has(player, offer.price())) return;
        if (!economy.withdrawPlayer(player, offer.price()).transactionSuccess()) return;
        final ItemStack bought = new ItemStack(item.getItem());
        bought.setAmount(offer.amount());
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(bought);
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        player.sendMessage(ChatColor.GREEN + "Compra realizada: " + offer.id() + ".");
    }

    private boolean isBlocked(String id) {
        final String normalized = id.toUpperCase(Locale.ROOT);
        return plugin.getConfig().getStringList("blocked-id-prefixes").stream().anyMatch(prefix -> normalized.startsWith(prefix.toUpperCase(Locale.ROOT)))
            || plugin.getConfig().getStringList("blocked-id-fragments").stream().anyMatch(fragment -> normalized.contains(fragment.toUpperCase(Locale.ROOT)));
    }

    private record Offer(String id, double price, int amount) { }
}
