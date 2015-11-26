/*
 * Copyright (c) 2015 CubeXMC. All Rights Reserved.
 * Created by PantherMan594.
 */

package net.cubexmc.infinichest;

import net.cubexmc.infinichest.Utils.Chests;
import net.cubexmc.infinichest.Utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by David on 10/03.
 *
 * @author David
 */
public class Main extends JavaPlugin implements Listener {
    public static Plugin plugin;
    public static HashMap<UUID, Settings> settingsMap = new HashMap<>();
    public static HashMap<UUID, HashMap<Integer, Inventory>> chestsMap = new HashMap<>();
    public static HashMap<UUID, Inventory> trashMap = new HashMap<>();
    public static HashMap<UUID, UUID> openChests = new HashMap<>();
    public static HashMap<UUID, ArrayList<UUID>> openedOthers = new HashMap<>();
    public static HashMap<UUID, ItemStack> tempItem = new HashMap<>();
    public static List<String> identifier = new ArrayList<>();

    @Override
    public void onEnable() {
        plugin = this;
        identifier.add(ChatColor.BLACK + "*");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!settingsMap.containsKey(p.getUniqueId())) {
                settingsMap.put(p.getUniqueId(), Settings.load(p.getUniqueId()));
                Chests.formatChests(p.getUniqueId(), p.getName());
            }
        }
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Settings.save(settingsMap.get(p.getUniqueId()));
            settingsMap.remove(p.getUniqueId());
            if (openedOthers.containsKey(p.getUniqueId())) {
                for (UUID uuid : openedOthers.get(p.getUniqueId())) {
                    Settings.save(settingsMap.get(uuid));
                    settingsMap.remove(uuid);
                }
            }
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!settingsMap.containsKey(p.getUniqueId())) {
            settingsMap.put(p.getUniqueId(), Settings.load(p.getUniqueId()));
            Chests.formatChests(p.getUniqueId(), p.getName());
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        Settings.save(settingsMap.get(e.getPlayer().getUniqueId()));
        settingsMap.remove(e.getPlayer().getUniqueId());
        if (openedOthers.containsKey(e.getPlayer().getUniqueId())) {
            for (UUID uuid : openedOthers.get(e.getPlayer().getUniqueId())) {
                Settings.save(settingsMap.get(uuid));
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
        if (openChests.containsKey(p.getUniqueId()) && e.getInventory().getName().contains("'s Chest p. ")) {
            Integer page = Integer.valueOf(e.getInventory().getName().replaceFirst("[\\S]+'s Chest p\\. ", ""));
            UUID openUuid = openChests.get(p.getUniqueId());
            HashMap<Integer, Inventory> chests = Main.chestsMap.get(openUuid);
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
            if (e.getClickedInventory().getName().contains("'s Chest p. ")) {
                e.setCancelled(true);
                Integer page = Integer.valueOf(e.getClickedInventory().getName().replaceFirst("[\\S]+'s Chest p\\. ", ""));
                HashMap<Integer, Inventory> chests;
                Settings settings;
                ItemStack item;
                ItemMeta meta;
                switch (e.getCurrentItem().getItemMeta().getDisplayName().replaceAll("[^\\s\\w\\d:]", "")) {
                    case "alPrevious Page":
                        ItemStack cursor = p.getItemOnCursor();
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
                        p.openInventory(chestsMap.get(owner).get(page - 1));
                        if (cursor != null) {
                            p.setItemOnCursor(cursor);
                        }
                        openChests.put(p.getUniqueId(), owner);
                        break;
                    case "alNext Page":
                        ItemStack cursor2 = p.getItemOnCursor();
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
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
                        e.getClickedInventory().setItem(49, item);
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
                        e.getClickedInventory().setItem(49, item);
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
                        e.getClickedInventory().setItem(49, item);
                        settings = settingsMap.get(owner);
                        settings.setAutoPickup(0);
                        settingsMap.put(owner, settings);
                        break;
                    case "6lChest":
                        if (p.getInventory().firstEmpty() >= 0) {
                            if (p.getInventory().contains(Material.CHEST)) {
                                for (int i = 0; i < 36; i++) {
                                    ItemStack stack = p.getInventory().getItem(i);
                                    if (stack != null && stack.getType().equals(Material.CHEST)) {
                                        stack.setAmount(stack.getAmount() - 1);
                                        p.getInventory().setItem(i, stack);
                                        ItemStack cStack = new ItemStack(Material.CHEST, 1);
                                        ItemMeta cMeta = cStack.getItemMeta();
                                        List<String> lore = new ArrayList<>();
                                        if (e.getClick().equals(ClickType.LEFT) || e.getClick().equals(ClickType.RIGHT)) {
                                            lore.add(ChatColor.GOLD + "Chest Withdrawal");
                                            lore.add(ChatColor.BLUE + "Place me and open!");
                                            lore.add(ChatColor.BLACK + owner.toString());
                                            lore.add(ChatColor.BLACK + page.toString());
                                        } else if (e.getClick().equals(ClickType.SHIFT_LEFT) || e.getClick().equals(ClickType.SHIFT_RIGHT)) {
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
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
                        Inventory trash = trashMap.get(p.getUniqueId());
                        ItemStack cursor3 = p.getItemOnCursor();
                        if (cursor3.getAmount() != 0) {
                            boolean done = false;
                            for (int j = 0; j < 54 && !done; j++) {
                                ItemStack trashItem = trash.getItem(j);
                                if (trashItem == null || trashItem.getType() == Material.AIR) {
                                    trash.setItem(j, p.getItemOnCursor());
                                    trashMap.put(p.getUniqueId(), trash);
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
                                        trashMap.put(p.getUniqueId(), trash);
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
                        Chests.setEmptyTrash(owner, e.getClickedInventory().getName().replace("'s Trash", ""));
                        break;
                }
            }
        }
    }

    public boolean onCommand(CommandSender se, Command c, String lbl, String[] args) {
        if (se instanceof Player) {
            Player p = (Player) se;
            if (args.length == 0) {
                if (!Main.settingsMap.containsKey(p.getUniqueId())) {
                    Main.settingsMap.put(p.getUniqueId(), Settings.load(p.getUniqueId()));
                    Chests.formatChests(p.getUniqueId(), p.getName());
                }
                p.openInventory(Main.chestsMap.get(p.getUniqueId()).get(settingsMap.get(p.getUniqueId()).getLastPage()));
                openChests.put(p.getUniqueId(), p.getUniqueId());
            } else {
                File f = new File(Main.plugin.getDataFolder() + File.separator + "playerdata" + File.separator + args[0] + ".yml");
                if (f.exists()) {
                    UUID uuid = UUID.fromString(args[0]);
                    if (!Main.settingsMap.containsKey(uuid)) {
                        Main.settingsMap.put(uuid, Settings.load(uuid));
                        Chests.formatChests(uuid, settingsMap.get(uuid).getName());
                    }
                    p.openInventory(Main.chestsMap.get(uuid).get(settingsMap.get(uuid).getLastPage()));
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
                    p.sendMessage(ChatColor.RED + "UUID not found. Format: /" + lbl + " [UUID].");
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
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && Chests.addItem(p.getUniqueId(), stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2 && addItemInventory(p, stack) > 0) {
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && Chests.addItem(p.getUniqueId(), stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent e) {
        ItemStack hand = e.getItemInHand();
        if (!e.isCancelled() && e.canBuild() && hand.getType().equals(Material.CHEST) && !hand.getItemMeta().getLore().isEmpty() && hand.getItemMeta().getLore().contains(identifier.get(0)) && hand.getItemMeta().getLore().contains(ChatColor.GOLD + "Chest Withdrawal")) {
            List<String> lore = hand.getItemMeta().getLore();
            UUID uuid = UUID.fromString(lore.get(2).substring(2));
            Integer page = Integer.valueOf(lore.get(3).substring(2));
            HashMap<Integer, Inventory> chests = chestsMap.get(uuid);
            Inventory chestInv = chests.get(page);
            Chest chest = (Chest) e.getBlock().getState();
            for (int i = 0; i < 45; i++) {
                if (chest.getInventory().firstEmpty() >= 0) {
                    if (chestInv.getContents()[i] != null) {
                        chest.getInventory().setItem(chest.getInventory().firstEmpty(), chestInv.getContents()[i]);
                        chestInv.clear(i);
                    }
                }
            }
        }
    }

    @EventHandler
    public void hopperTransfer(InventoryMoveItemEvent e) {
        if (!e.isCancelled() && e.getDestination().getName().startsWith(ChatColor.BLACK + "h;")) {
            Chests.addItem(UUID.fromString(e.getDestination().getName().split(";")[1]), e.getItem());
            final Inventory inv = e.getDestination();
            Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (inv.firstEmpty() != 0) {
                            inv.clear();
                            break;
                        }
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
                Chests.addItem(p.getUniqueId(), stack);
                e.getItem().remove();
                e.setCancelled(true);
            } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2) {
                stack.setAmount(addItemInventory(p, stack));
                if (stack.getAmount() > 0) {
                    Chests.addItem(p.getUniqueId(), stack);
                    e.getItem().remove();
                    e.setCancelled(true);
                }
            }
        }
    }

    public boolean checkNearbyItem(Player p) {
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
}
