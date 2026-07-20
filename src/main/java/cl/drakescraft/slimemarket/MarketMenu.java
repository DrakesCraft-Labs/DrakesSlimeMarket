package cl.drakescraft.slimemarket;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class MarketMenu implements CommandExecutor, TabCompleter, Listener {
    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM HH:mm")
        .withZone(ZoneId.of("America/Santiago"));

    private final DrakesSlimeMarket plugin;
    private final Economy economy;
    private final MarketCatalog catalog;
    private final DynamicPricing pricing;
    private final MarketAuditLogger auditLogger;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    MarketMenu(
        DrakesSlimeMarket plugin,
        Economy economy,
        MarketCatalog catalog,
        DynamicPricing pricing,
        MarketAuditLogger auditLogger
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.catalog = catalog;
        this.pricing = pricing;
        this.auditLogger = auditLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("drakesslimemarket.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para recargar el mercado.");
                return true;
            }
            plugin.reloadMarket();
            sender.sendMessage(ChatColor.GREEN + "Catalogo y precios del mercado recargados.");
            return true;
        }

        if (args.length > 0 && "stats".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("drakesslimemarket.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para ver la economia interna.");
                return true;
            }
            sendStats(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede abrir el mercado.");
            return true;
        }
        if (catalog.entries().isEmpty()) {
            plugin.refreshMarket();
        }
        open(player, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("drakesslimemarket.admin")) {
            return List.of("reload", "stats").stream()
                .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    /** Abre una sesion aislada: los slots de un jugador nunca se comparten con otro menu. */
    private void open(Player player, int requestedPage) {
        final List<CatalogEntry> allEntries = catalog.entries();
        final int pageCount = Math.max(1, (int) Math.ceil(allEntries.size() / (double) PAGE_SIZE));
        final int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        final String configuredTitle = plugin.getConfig().getString("catalog.title", "&0Mercado Slimefun");
        final String title = color(configuredTitle
            .replace("%page%", Integer.toString(page + 1))
            .replace("%pages%", Integer.toString(pageCount)));
        final MarketInventory holder = new MarketInventory(title, page, pageCount);
        final Inventory inventory = holder.getInventory();

        final int from = page * PAGE_SIZE;
        final int to = Math.min(allEntries.size(), from + PAGE_SIZE);
        for (int index = from; index < to; index++) {
            final CatalogEntry entry = allEntries.get(index);
            final int slot = index - from;
            inventory.setItem(slot, createOfferIcon(entry));
            holder.offers.put(slot, entry.id());
        }

        if (allEntries.isEmpty()) {
            inventory.setItem(22, named(Material.BARRIER, ChatColor.RED + "No hay materiales habilitados",
                List.of(ChatColor.GRAY + "Revisa las reglas del catalogo o los addons cargados.")));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, named(Material.ARROW, ChatColor.GOLD + "Pagina anterior", List.of()));
        }
        inventory.setItem(INFO_SLOT, marketInfo(allEntries.size()));
        if (page + 1 < pageCount) {
            inventory.setItem(NEXT_SLOT, named(Material.ARROW, ChatColor.GOLD + "Pagina siguiente", List.of()));
        }
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarketInventory holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0) {
            return;
        }
        if (event.getRawSlot() == PREVIOUS_SLOT && holder.page > 0) {
            open(player, holder.page - 1);
            return;
        }
        if (event.getRawSlot() == NEXT_SLOT && holder.page + 1 < holder.pageCount) {
            open(player, holder.page + 1);
            return;
        }

        final String id = holder.offers.get(event.getRawSlot());
        if (id == null) {
            return;
        }
        catalog.find(id).ifPresent(entry -> purchase(player, entry, event.isShiftClick() ? entry.maxStackSize() : 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MarketInventory) {
            event.setCancelled(true);
        }
    }

    /** Cobra y entrega dentro del mismo tick; ante cualquier sobrante restaura inventario y dinero. */
    private void purchase(Player player, CatalogEntry entry, int amount) {
        final long now = System.currentTimeMillis();
        final long cooldownMillis = Math.max(0L, plugin.getConfig().getLong("purchase-cooldown-seconds", 2L)) * 1000L;
        final long availableAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < availableAt) {
            fail(player, "Espera " + Math.max(1L, (availableAt - now + 999L) / 1000L) + " s antes de otra compra.");
            return;
        }

        final ItemStack bought = entry.createItem(amount);
        if (!canFit(player.getInventory(), bought)) {
            fail(player, "No tienes espacio para recibir esa compra.");
            return;
        }

        final double unitPrice = pricing.unitPrice(entry);
        final double total = Math.round(unitPrice * amount * 100.0D) / 100.0D;
        if (!economy.has(player, total)) {
            fail(player, "Necesitas " + economy.format(total) + ".");
            return;
        }

        final ItemStack[] before = cloneContents(player.getInventory().getStorageContents());
        final EconomyResponse withdrawal = economy.withdrawPlayer(player, total);
        if (!withdrawal.transactionSuccess()) {
            fail(player, "La economia rechazo la compra: " + withdrawal.errorMessage);
            return;
        }

        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(bought);
        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(before);
            final EconomyResponse refund = economy.depositPlayer(player, total);
            plugin.getLogger().severe("Compra revertida para " + player.getName() + " por sobrantes; reembolso="
                + refund.transactionSuccess());
            fail(player, "La entrega no fue segura y el cobro fue revertido.");
            return;
        }

        cooldowns.put(player.getUniqueId(), now + cooldownMillis);
        pricing.registerPurchase(entry.id(), amount);
        auditLogger.purchase(player, entry, amount, unitPrice, total, economy.getBalance(player));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.15F);
        player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + entry.displayName()
            + ChatColor.GREEN + " por " + ChatColor.GOLD + economy.format(total) + ChatColor.GREEN + ".");
    }

    private ItemStack createOfferIcon(CatalogEntry entry) {
        final ItemStack icon = entry.createItem(1);
        final ItemMeta meta = icon.getItemMeta();
        final double price = pricing.unitPrice(entry);
        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + entry.id());
        lore.add(ChatColor.GRAY + "Addon: " + ChatColor.AQUA + entry.addon());
        lore.add("");
        lore.add(ChatColor.GOLD + "Unidad: " + ChatColor.WHITE + economy.format(price));
        lore.add(ChatColor.GOLD + "Stack x" + entry.maxStackSize() + ": " + ChatColor.WHITE
            + economy.format(price * entry.maxStackSize()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click: comprar una unidad");
        lore.add(ChatColor.LIGHT_PURPLE + "Shift + click: comprar un stack");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack marketInfo(int itemCount) {
        final DynamicPricing.MarketStats stats = pricing.stats();
        final String refresh = stats.refreshedAt() == 0L ? "pendiente" : TIME_FORMAT.format(Instant.ofEpochSecond(stats.refreshedAt()));
        return named(Material.CLOCK, ChatColor.GOLD + "Mercado dinamico", List.of(
            ChatColor.GRAY + "Materiales: " + ChatColor.WHITE + itemCount,
            ChatColor.GRAY + "Circulacion observada: " + ChatColor.WHITE + economy.format(stats.totalWealth()),
            ChatColor.GRAY + "Depositos sBank: " + ChatColor.WHITE + economy.format(stats.bankWealth()),
            ChatColor.GRAY + "Ventana de precios: " + ChatColor.WHITE + refresh,
            ChatColor.DARK_GRAY + "Los precios se recalculan cada 30 minutos."
        ));
    }

    private void sendStats(CommandSender sender) {
        final DynamicPricing.MarketStats stats = pricing.stats();
        sender.sendMessage(ChatColor.GOLD + "DrakesSlimeMarket" + ChatColor.GRAY + " | ofertas: " + stats.pricedItems());
        sender.sendMessage(ChatColor.GRAY + "Circulacion observada: " + economy.format(stats.totalWealth())
            + " | sBank: " + economy.format(stats.bankWealth()));
    }

    private void fail(Player player, String message) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
        player.sendMessage(ChatColor.RED + message);
    }

    private static boolean canFit(PlayerInventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                remaining -= Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        return Arrays.stream(contents).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private static final class MarketInventory implements InventoryHolder {
        private final Inventory inventory;
        private final int page;
        private final int pageCount;
        private final Map<Integer, String> offers = new HashMap<>();

        private MarketInventory(String title, int page, int pageCount) {
            this.page = page;
            this.pageCount = pageCount;
            this.inventory = Bukkit.createInventory(this, 54, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
