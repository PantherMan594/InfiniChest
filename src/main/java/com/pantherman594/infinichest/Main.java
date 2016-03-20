/*
 * Copyright (c) 2016 David Shen. All Rights Reserved.
 * Created by PantherMan594.
 */

package com.pantherman594.infinichest;

import com.evilmidget38.UUIDFetcher;
import com.pantherman594.infinichest.Utils.Chests;
import com.pantherman594.infinichest.Utils.Settings;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by David on 10/03.
 *
 * @author David
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Main extends JavaPlugin implements Listener {
    private static Main instance;
    private FileConfiguration config;
    private String format;
    private String formatStrippedRegEx;
    private HashMap<UUID, Settings> settingsMap = new HashMap<>();
    private HashMap<UUID, HashMap<Integer, Inventory>> chestsMap = new HashMap<>();
    private HashMap<UUID, Inventory> trashMap = new HashMap<>();
    private HashMap<UUID, UUID> openChests = new HashMap<>();
    private HashMap<UUID, ArrayList<UUID>> openedOthers = new HashMap<>();
    private HashMap<UUID, ItemStack> tempItem = new HashMap<>();
    private List<String> identifier = new ArrayList<>();
    private boolean chestWithdraw;
    private boolean hopperTransfer;
    private Integer rows;
    private Integer saveInterval;

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        instance = this;
        config = super.getConfig();
        format = config.getString("title", "[name]'s Chest p. [page]");
        String formatStripped = format.replace("[name]", "").replace("[page]", "").replace("&", "§");
        if (formatStripped.length() > 28) {
            formatStripped = "'s Chest p. ";
            getLogger().warning("Chest title too long, chest titles can be a maximum of 32 characters (including colors, names, and page numbers).");
        }
        formatStrippedRegEx = formatStripped.replace("\\", "\\\\").replace(".", "\\.").replace("(", "\\(").replace(")", "\\)").replace("?", "\\?");
        identifier.add(ChatColor.BLACK + "*");
        saveInterval = config.getInt("saveInterval", 300);
        chestWithdraw = config.getBoolean("chestWithdraw", true);
        hopperTransfer = config.getBoolean("hopperTransfer", true);
        rows = config.getInt("rows", 6);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            settingsMap.put(p.getUniqueId(), new Settings(p.getUniqueId()));
            new Chests().formatChests(p.getUniqueId(), p.getName(), settingsMap.get(p.getUniqueId()).getLastPage());
        }
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            settingsMap.get(p.getUniqueId()).save();
            settingsMap.remove(p.getUniqueId());
            if (openedOthers.containsKey(p.getUniqueId())) {
                for (UUID uuid : openedOthers.get(p.getUniqueId())) {
                    settingsMap.get(uuid).save();
                    settingsMap.remove(uuid);
                }
            }
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        settingsMap.put(p.getUniqueId(), new Settings(p.getUniqueId()));
        new Chests().formatChests(p.getUniqueId(), p.getName(), settingsMap.get(p.getUniqueId()).getLastPage());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        settingsMap.get(e.getPlayer().getUniqueId()).save();
        settingsMap.remove(e.getPlayer().getUniqueId());
        if (openedOthers.containsKey(e.getPlayer().getUniqueId())) {
            for (UUID uuid : openedOthers.get(e.getPlayer().getUniqueId())) {
                settingsMap.get(uuid).save();
                settingsMap.remove(uuid);
            }
        }
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (settingsMap.get(p.getUniqueId()).getAutoPickup() > 0) {
            if (checkNearbyItem(p) && p.getInventory().firstEmpty() < 0) {
                tempItem.put(p.getUniqueId(), p.getInventory().getItem(9));
                p.getInventory().clear(9);
            } else {
                if (tempItem.containsKey(p.getUniqueId())) {
                    p.getInventory().setItem(9, tempItem.get(p.getUniqueId()));
                    tempItem.remove(p.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void open(InventoryOpenEvent e) {
        if (!e.isCancelled() && e.getInventory().getName().startsWith(ChatColor.BLACK + "h;")) {
            e.getPlayer().sendMessage(ChatColor.RED + "Sorry you can't open an InfiniChest chest (there's nothing here anyways).");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (openChests.containsKey(p.getUniqueId()) && !e.getInventory().getName().endsWith("'s Trash")) {
            Integer page = Integer.valueOf(e.getInventory().getName().replaceFirst("[\\S]+" + formatStrippedRegEx, ""));
            UUID openUuid = openChests.get(p.getUniqueId());
            HashMap<Integer, Inventory> chests = chestsMap.get(openUuid);
            chests.put(page, e.getInventory());
            Settings settings = settingsMap.get(openUuid);
            settings.setLastPage(page);
            settingsMap.put(openUuid, settings);
            chestsMap.put(openUuid, chests);
            openChests.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void interact(InventoryClickEvent e) {
        if (!e.isCancelled() && e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getLore() != null && !e.getCurrentItem().getItemMeta().getLore().isEmpty() && e.getCurrentItem().getItemMeta().getLore().contains(identifier.get(0))) {
            Player p = (Player) e.getWhoClicked();
            UUID owner = openChests.get(p.getUniqueId());
            if (openChests.containsKey(p.getUniqueId()) && !e.getInventory().getName().endsWith("'s Trash")) {
                e.setCancelled(true);
                Integer page = Integer.valueOf(e.getClickedInventory().getName().replaceFirst("[\\S]+" + formatStrippedRegEx, ""));
                HashMap<Integer, Inventory> chests;
                Settings settings;
                ItemStack item;
                ItemMeta meta;
                switch (e.getCurrentItem().getItemMeta().getDisplayName().replaceAll("[^\\s\\w\\d:]", "")) {
                    case "alPrevious Page":
                        ItemStack cursor = p.getItemOnCursor();
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                        chests = chestsMap.get(owner);
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(owner, chests);
                        if (!chestsMap.get(owner).containsKey(page - 1)) {
                            new Chests().formatChests(owner, settingsMap.get(owner).getName(), page - 1);
                        }
                        p.openInventory(chestsMap.get(owner).get(page - 1));
                        if (cursor != null) {
                            p.setItemOnCursor(cursor);
                        }
                        openChests.put(p.getUniqueId(), owner);
                        break;
                    case "alNext Page":
                        ItemStack cursor2 = p.getItemOnCursor();
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                        chests = chestsMap.get(owner);
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(owner, chests);
                        if (!chestsMap.get(owner).containsKey(page + 1)) {
                            new Chests().formatChests(owner, settingsMap.get(owner).getName(), page + 1);
                        }
                        p.openInventory(chestsMap.get(owner).get(page + 1));
                        if (cursor2 != null) {
                            p.setItemOnCursor(cursor2);
                        }
                        openChests.put(p.getUniqueId(), owner);
                        break;
                    case "blAuto Pickup: Disabled":
                        item = new ItemStack(Material.HOPPER, 1);
                        meta = item.getItemMeta();
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: All Items");
                        meta.setLore(identifier);
                        item.setItemMeta(meta);
                        e.getClickedInventory().setItem(58, item);
                        settings = settingsMap.get(owner);
                        settings.setAutoPickup(1);
                        settingsMap.put(owner, settings);
                        break;
                    case "blAuto Pickup: All Items":
                        item = new ItemStack(Material.HOPPER, 1);
                        meta = item.getItemMeta();
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: On Full Inventory");
                        meta.setLore(identifier);
                        item.setItemMeta(meta);
                        e.getClickedInventory().setItem(58, item);
                        settings = settingsMap.get(owner);
                        settings.setAutoPickup(2);
                        settingsMap.put(owner, settings);
                        break;
                    case "blAuto Pickup: On Full Inventory":
                        item = new ItemStack(Material.HOPPER, 1);
                        meta = item.getItemMeta();
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: Disabled");
                        meta.setLore(identifier);
                        item.setItemMeta(meta);
                        e.getClickedInventory().setItem(58, item);
                        settings = settingsMap.get(owner);
                        settings.setAutoPickup(0);
                        settingsMap.put(owner, settings);
                        break;
                    case "6lChest":
                        if ((chestWithdraw || hopperTransfer) && p.getInventory().firstEmpty() >= 0) {
                            if (p.getInventory().contains(Material.CHEST)) {
                                for (int i = 0; i < 36; i++) {
                                    ItemStack stack = p.getInventory().getItem(i);
                                    if (stack != null && stack.getType().equals(Material.CHEST)) {
                                        stack.setAmount(stack.getAmount() - 1);
                                        p.getInventory().setItem(i, stack);
                                        ItemStack cStack = new ItemStack(Material.CHEST, 1);
                                        ItemMeta cMeta = cStack.getItemMeta();
                                        List<String> lore = new ArrayList<>();
                                        if ((e.getClick().equals(ClickType.LEFT) || e.getClick().equals(ClickType.RIGHT)) && chestWithdraw) {
                                            lore.add(ChatColor.GOLD + "Chest Withdrawal");
                                            lore.add(ChatColor.BLUE + "Place me and open!");
                                            lore.add(ChatColor.BLACK + owner.toString());
                                            lore.add(ChatColor.BLACK + page.toString());
                                        } else if ((e.getClick().equals(ClickType.SHIFT_LEFT) || e.getClick().equals(ClickType.SHIFT_RIGHT)) && hopperTransfer) {
                                            cMeta.setDisplayName(ChatColor.BLACK + "h;" + owner.toString());
                                            lore.add(ChatColor.GOLD + "Hopper Chest");
                                            lore.add(ChatColor.BLUE + "Place me and attach a hopper!");
                                        }
                                        lore.addAll(identifier);
                                        cMeta.setLore(lore);
                                        cStack.setItemMeta(cMeta);
                                        p.getInventory().setItem(p.getInventory().firstEmpty(), cStack);
                                        i = 36;
                                    }
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "Error: Make sure you have a chest in your inventory first!");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "Error: Please have an open spot in your inventory to withdraw a chest.");
                        }
                        break;
                    case "clTRASH":
                        chests = chestsMap.get(owner);
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(owner, chests);
                        Inventory trash = trashMap.get(owner);
                        ItemStack cursor3 = p.getItemOnCursor();
                        if (cursor3.getAmount() != 0) {
                            boolean done = false;
                            for (int j = 0; j < 54 && !done; j++) {
                                ItemStack trashItem = trash.getItem(j);
                                if (trashItem == null || trashItem.getType() == Material.AIR) {
                                    trash.setItem(j, p.getItemOnCursor());
                                    trashMap.put(owner, trash);
                                    cursor3.setAmount(0);
                                    done = true;
                                } else if (trashItem != null && trashItem.isSimilar(cursor3)) {
                                    int amt = trashItem.getAmount() + cursor3.getAmount();
                                    int max = trashItem.getMaxStackSize();
                                    if (amt > max) {
                                        trashItem.setAmount(max);
                                        cursor3.setAmount(amt - max);
                                    } else {
                                        trashItem.setAmount(amt);
                                        trash.setItem(j, trashItem);
                                        trashMap.put(owner, trash);
                                        cursor3.setAmount(0);
                                        done = true;
                                    }
                                }
                            }
                            p.setItemOnCursor(cursor3);
                        } else {
                            p.openInventory(trash);
                            openChests.put(p.getUniqueId(), owner);
                        }
                        break;
                }
            } else if (e.getClickedInventory().getName().endsWith("'s Trash")) {
                e.setCancelled(true);
                switch (e.getCurrentItem().getItemMeta().getDisplayName().replaceAll("[^\\s\\w\\d:]", "")) {
                    case "lExit Trash":
                        trashMap.put(owner, e.getClickedInventory());
                        p.openInventory(chestsMap.get(owner).get(settingsMap.get(owner).getLastPage()));
                        openChests.put(p.getUniqueId(), owner);
                        break;
                    case "clEmpty Trash":
                        p.closeInventory();
                        new Chests().setEmptyTrash(owner, e.getClickedInventory().getName().replace("'s Trash", ""));
                        break;
                }
            }
        }
    }

    public boolean onCommand(CommandSender se, Command c, String lbl, String[] args) {
        if (se instanceof Player) {
            Player p = (Player) se;
            if (args.length == 0) {
                if (!settingsMap.containsKey(p.getUniqueId())) {
                    settingsMap.put(p.getUniqueId(), new Settings(p.getUniqueId()));
                    new Chests().formatChests(p.getUniqueId(), p.getName(), settingsMap.get(p.getUniqueId()).getLastPage());
                }
                if (settingsMap.get(p.getUniqueId()).getMax() != 0) {
                    p.openInventory(chestsMap.get(p.getUniqueId()).get(settingsMap.get(p.getUniqueId()).getLastPage()));
                    openChests.put(p.getUniqueId(), p.getUniqueId());
                } else {
                    p.sendMessage(ChatColor.RED + "Error: You do not have permission to use any chests!");
                }
            } else {
                if (p.hasPermission("InfiniChest.others")) {
                    UUID uuid = null;
                    String dir = getDataFolder() + File.separator + "playerdata" + File.separator;
                    if (new File(dir + args[0] + ".yml").exists()) {
                        uuid = UUID.fromString(args[0]);
                    } else {
                        UUID uuidTemp = nameToUUID(args[0]);
                        if (uuidTemp != null && new File(dir + uuidTemp.toString() + ".yml").exists()) {
                            uuid = uuidTemp;
                        }
                    }
                    if (uuid != null) {
                        if (!settingsMap.containsKey(uuid)) {
                            settingsMap.put(uuid, new Settings(uuid));
                            new Chests().formatChests(uuid, settingsMap.get(uuid).getName(), settingsMap.get(uuid).getLastPage());
                        }
                        p.openInventory(chestsMap.get(uuid).get(settingsMap.get(uuid).getLastPage()));
                        openChests.put(p.getUniqueId(), uuid);
                        ArrayList<UUID> list = new ArrayList<>();
                        if (openedOthers.containsKey(p.getUniqueId())) {
                            list = openedOthers.get(p.getUniqueId());
                        }
                        if (!list.contains(uuid)) {
                            list.add(uuid);
                        }
                        openedOthers.put(p.getUniqueId(), list);
                    } else {
                        p.sendMessage(ChatColor.RED + "Player not found. Usage: /" + lbl + " [UUID|Name].");
                    }
                }
            }
        } else {
            se.sendMessage(ChatColor.RED + "Console may not execute this command!");
        }
        return true;
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (settingsMap.get(p.getUniqueId()).getAutoPickup() > 0 && !e.isCancelled()) {
            for (ItemStack stack : e.getBlock().getDrops()) {
                int amount = (int) (Math.random() * (double) p.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS));
                if (amount > 1 & !stack.getType().isBlock()) {
                    stack.setAmount(amount);
                }
                if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 1) {
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && new Chests().addItem(p.getUniqueId(), stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2 && addItemInventory(p, stack) > 0) {
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && new Chests().addItem(p.getUniqueId(), stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockPlace(BlockPlaceEvent e) {
        ItemStack hand = e.getItemInHand();
        if (!e.isCancelled() && e.canBuild() && hand.getType().equals(Material.CHEST) && !hand.getItemMeta().getLore().isEmpty() && hand.getItemMeta().getLore().contains(identifier.get(0)) && hand.getItemMeta().getLore().contains(ChatColor.GOLD + "Chest Withdrawal")) {
            List<String> lore = hand.getItemMeta().getLore();
            UUID uuid = UUID.fromString(lore.get(2).substring(2));
            Integer page = Integer.valueOf(lore.get(3).substring(2));
            HashMap<Integer, Inventory> chests = chestsMap.get(uuid);
            final Inventory chestInv = chests.get(page);
            final Location loc = e.getBlock().getLocation();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (loc.getBlock().getState() instanceof Chest) {
                    Chest chest = (Chest) loc.getBlock().getState();
                    int i = 0;
                    while (chest.getInventory().firstEmpty() >= 0) {
                        if (chestInv.getContents()[i] != null) {
                            chest.getInventory().setItem(chest.getInventory().firstEmpty(), chestInv.getContents()[i]);
                            chestInv.clear(i);
                        }
                        i++;
                    }
                } else {
                    getLogger().info("not chest");
                }
            }, 20);
        }
    }

    @EventHandler
    public void hopperTransfer(InventoryMoveItemEvent e) {
        if (!e.isCancelled() && e.getDestination().getName().startsWith(ChatColor.BLACK + "h;")) {
            new Chests().addItem(UUID.fromString(e.getDestination().getName().split(";")[1]), e.getItem());
            final Inventory inv = e.getDestination();
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                while (true) {
                    if (inv.firstEmpty() != 0) {
                        inv.clear();
                        return;
                    }
                }
            });
        }
    }

    @EventHandler
    public void pickupItem(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (settingsMap.get(p.getUniqueId()).getAutoPickup() > 0 && !e.isCancelled()) {
            ItemStack stack = e.getItem().getItemStack();
            if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 1) {
                new Chests().addItem(p.getUniqueId(), stack);
                e.getItem().remove();
                e.setCancelled(true);
            } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2) {
                stack.setAmount(addItemInventory(p, stack));
                if (stack.getAmount() > 0) {
                    new Chests().addItem(p.getUniqueId(), stack);
                    e.getItem().remove();
                    e.setCancelled(true);
                }
            }
        }
    }

    private boolean checkNearbyItem(Player p) {
        for (Entity e : p.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                return true;
            }
        }
        return false;
    }

    public Integer addItemInventory(Player p, ItemStack stack) {
        if (p.getInventory().firstEmpty() < 0) {
            for (int i = 0; i < 36 && i != 9; i++) {
                ItemStack item = p.getInventory().getItem(i);
                if (item != null && item.isSimilar(stack)) {
                    int amt = item.getAmount() + stack.getAmount();
                    int max = item.getMaxStackSize();
                    if (amt > max) {
                        item.setAmount(max);
                        stack.setAmount(amt - max);
                    } else {
                        item.setAmount(amt);
                        return 0;
                    }
                }
            }
            return stack.getAmount();
        }
        return 0;
    }

    private UUID nameToUUID(String name) {
        if (name == null) {
            return null;
        } else {
            UUID id = Bukkit.getServer().getOfflinePlayer(name).getUniqueId();
            if (Bukkit.getPlayer(name) != null) {
                id = Bukkit.getPlayer(name).getUniqueId();
            } else if (Bukkit.getServer().getOnlineMode()) {
                UUIDFetcher fetcher = new UUIDFetcher(Collections.singletonList(name));
                try {
                    Map e1 = fetcher.call();

                    for (Object o : e1.entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        if (name.equalsIgnoreCase((String) entry.getKey())) {
                            id = (UUID) entry.getValue();
                            break;
                        }
                    }
                } catch (Exception e) {
                    this.getLogger().log(Level.SEVERE, "Exception on online UUID fetch", e);
                }
            }

            return id;
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    public String getFormat() {
        return format;
    }

    public String getFormatStrippedRegEx() {
        return formatStrippedRegEx;
    }

    public HashMap<UUID, Settings> getSettingsMap() {
        return settingsMap;
    }

    public HashMap<UUID, HashMap<Integer, Inventory>> getChestsMap() {
        return chestsMap;
    }

    public HashMap<UUID, Inventory> getTrashMap() {
        return trashMap;
    }

    public List<String> getIdentifier() {
        return identifier;
    }

    public boolean isChestWithdraw() {
        return chestWithdraw;
    }

    public boolean isHopperTransfer() {
        return hopperTransfer;
    }

    public Integer getRows() {
        return rows;
    }

    public Integer getSaveInterval() {
        return saveInterval;
    }
}
