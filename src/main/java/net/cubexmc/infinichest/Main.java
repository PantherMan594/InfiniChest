/*
 * Copyright (c) 2015 David Shen. All Rights Reserved.
 * Created by PantherMan594.
 */

package net.cubexmc.infinichest;

import net.cubexmc.infinichest.Utils.Chests;
import net.cubexmc.infinichest.Utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

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
    public static List<String> identifier = new ArrayList<>();

    @Override
    public void onEnable() {
        plugin = this;
        identifier.add(ChatColor.BLACK + "*");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Settings.save(settingsMap.get(p.getUniqueId()));
            settingsMap.remove(p.getUniqueId());
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
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (openChests.containsKey(p.getUniqueId())) {
            if (e.getInventory().getName().contains("'s Chest (Page ")) {
                Integer page = Integer.valueOf(e.getInventory().getName().replaceFirst("[\\W\\w]+\\(Page ", "").replace(")", ""));
                HashMap<Integer, Inventory> chests = Main.chestsMap.get(p.getUniqueId());
                chests.put(page, e.getInventory());
                Settings settings = settingsMap.get(p.getUniqueId());
                settings.setLastPage(page);
                settingsMap.put(p.getUniqueId(), settings);
                chestsMap.put(p.getUniqueId(), chests);
                openChests.remove(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void interact(InventoryClickEvent e) {
        if (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getLore() != null && !e.getCurrentItem().getItemMeta().getLore().isEmpty() && e.getCurrentItem().getItemMeta().getLore().get(0).contains("*")) {
            Player p = (Player) e.getWhoClicked();
            UUID owner = openChests.get(p.getUniqueId());
            if (e.getClickedInventory().getName().contains("'s Chest (Page ")) {
                e.setCancelled(true);
                Integer page = Integer.valueOf(e.getClickedInventory().getName().replaceFirst("[\\W\\w]+\\(Page ", "").replace(")", ""));
                HashMap<Integer, Inventory> chests;
                Settings settings;
                ItemStack item;
                ItemMeta meta;
                switch (e.getCurrentItem().getItemMeta().getDisplayName().replaceAll("[^\\s\\w\\d:]", "")) {
                    case "alPrevious Page":
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
                        p.openInventory(chestsMap.get(owner).get(page - 1));
                        openChests.put(p.getUniqueId(), owner);
                        break;
                    case "alNext Page":
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
                        p.openInventory(chestsMap.get(owner).get(page + 1));
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
                    case "clTRASH":
                        chests = Main.chestsMap.get(p.getUniqueId());
                        chests.put(page, e.getClickedInventory());
                        chestsMap.put(p.getUniqueId(), chests);
                        Inventory trash = trashMap.get(p.getUniqueId());
                        ItemStack cursor = p.getItemOnCursor();
                        if (cursor.getAmount() != 0) {
                            boolean done = false;
                            for (int j = 0; j < 54 && !done; j++) {
                                ItemStack trashItem = trash.getItem(j);
                                if (trashItem == null || trashItem.getType() == Material.AIR) {
                                    trash.setItem(j, p.getItemOnCursor());
                                    trashMap.put(p.getUniqueId(), trash);
                                    cursor.setAmount(0);
                                    done = true;
                                } else if (trashItem != null && trashItem.isSimilar(cursor)) {
                                    int amt = trashItem.getAmount() + cursor.getAmount();
                                    int max = trashItem.getMaxStackSize();
                                    if (amt > max) {
                                        trashItem.setAmount(max);
                                        cursor.setAmount(amt - max);
                                    } else {
                                        trashItem.setAmount(amt);
                                        trash.setItem(j, trashItem);
                                        trashMap.put(p.getUniqueId(), trash);
                                        cursor.setAmount(0);
                                        done = true;
                                    }
                                }
                            }
                            p.setItemOnCursor(cursor);
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
                p.openInventory(Main.chestsMap.get(p.getUniqueId()).get(settingsMap.get(p.getUniqueId()).getLastPage()));
                openChests.put(p.getUniqueId(), p.getUniqueId());
            } else {
                File f = new File(Main.plugin.getDataFolder() + File.separator + "playedata" + File.separator + args[0] + ".yml");
                if (f.exists()) {
                    UUID uuid = UUID.fromString(args[0]);
                    if (!Main.settingsMap.containsKey(uuid)) {
                        Main.settingsMap.put(uuid, Settings.load(uuid));
                        Chests.formatChests(uuid, settingsMap.get(uuid).getName());
                    }
                    p.openInventory(Main.chestsMap.get(uuid).get(settingsMap.get(uuid).getLastPage()));
                    openChests.put(p.getUniqueId(), uuid);
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
            Iterator drops = e.getBlock().getDrops().iterator();
            while (drops.hasNext()) {
                ItemStack stack = (ItemStack) drops.next();
                int amount = (int) (Math.random() * (double) p.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS));
                if (amount > 1 & !stack.getType().isBlock()) {
                    stack.setAmount(amount);
                }
                if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 1) {
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && Chests.addItem(p, stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2 && addItemInventory(p, stack) > 0) {
                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && Chests.addItem(p, stack)) {
                        e.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler
    public void pickupItem(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (settingsMap.get(p.getUniqueId()).getAutoPickup() > 0 && !e.isCancelled()) {
            ItemStack stack = e.getItem().getItemStack();
            if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 1) {
                Chests.addItem(p, stack);
                e.getItem().remove();
                e.setCancelled(true);
            } else if (settingsMap.get(p.getUniqueId()).getAutoPickup() == 2) {
                stack.setAmount(addItemInventory(p, stack));
                if (stack.getAmount() > 0) {
                    Chests.addItem(p, stack);
                    e.getItem().remove();
                    e.setCancelled(true);
                }
            }
        }
    }

    public Integer addItemInventory(Player p, ItemStack stack) {
        if (p.getInventory().firstEmpty() < 0) {
            for (int i = 0; i < 36; i++) {
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
