/*
 * Copyright (c) 2016 David Shen. All Rights Reserved.
 * Created by PantherMan594.
 */

package com.pantherman594.infinichest.Utils;

import com.pantherman594.infinichest.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by David on 10/03.
 *
 * @author David
 */
public class Chests {

    public boolean addItem(UUID uuid, ItemStack stack) {
        HashMap<Integer, Inventory> chests = Main.getInstance().getChestsMap().get(uuid);
        int i = 1;
        while (true) {
            if (Main.getInstance().getSettingsMap().get(uuid).getMax() < i) {
                return false;
            }
            Inventory chest = chests.get(i);
            formatChests(uuid, Main.getInstance().getSettingsMap().get(uuid).getName(), i);
            if (chest != null) {
                for (int j = 0; j < 9 * Main.getInstance().getRows(); j++) {
                    ItemStack item = chest.getItem(j);
                    if (item == null || item.getType() == Material.AIR) {
                        chest.setItem(j, stack);
                        chests.put(i, chest);
                        Main.getInstance().getChestsMap().put(uuid, chests);
                        return true;
                    } else if (item != null && item.isSimilar(stack)) {
                        int amt = item.getAmount() + stack.getAmount();
                        int max = item.getMaxStackSize();
                        if (amt > max) {
                            item.setAmount(max);
                            stack.setAmount(amt - max);
                        } else {
                            item.setAmount(amt);
                            chest.setItem(j, item);
                            chests.put(i, chest);
                            Main.getInstance().getChestsMap().put(uuid, chests);
                            return true;
                        }
                    }
                }
            }
            i++;
        }
    }

    // 0  1  2  3  4  5  6  7  8
    // 9 10 11 12 13 14 15 16 17
    //18 19 20 21 22 23 24 25 26
    //27 28 29 30 31 32 33 34 35
    //36 37 38 39 40 41 42 43 44
    //45 46 47 48 49 50 51 52 53
    //54 55 56 57 58 59 60 61 62

    public void formatChests(UUID uuid, String name, int i) {
        HashMap<Integer, Inventory> chests;
        if (Main.getInstance().getChestsMap().containsKey(uuid)) {
            chests = Main.getInstance().getChestsMap().get(uuid);
            if (chests.get(i) != null) {
                return;
            }
        } else {
            chests = new HashMap<>();
        }
        String title = Main.color(Main.getInstance().getFormat().replace("[name]", name).replace("[page]", "" + i));
        while (title.length() > 32) {
            title = title.replace(name, name.substring(0, name.length() - 1));
        }
        if (String.valueOf(i).endsWith("0")) {
            Bukkit.getLogger().info(title);
        }
        Inventory chest = Bukkit.createInventory(null, 9 * Main.getInstance().getRows() + 9, title);
        if (Main.getInstance().getSettingsMap().get(uuid).getItems().containsKey(i)) {
            chest.setContents(Main.getInstance().getSettingsMap().get(uuid).getItems().get(i));
        }
        for (int j = 9 * Main.getInstance().getRows(); j < 9 * Main.getInstance().getRows() + 9; j++) {
            ItemStack item = null;
            ItemMeta meta = null;
            List<String> lore = new ArrayList<>();
            if (j < 9 * Main.getInstance().getRows() + 3) {
                if (i > 1) {
                    item = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.LIME.getData());
                    meta = item.getItemMeta();
                    meta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + "Previous Page");
                } else {
                    item = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getData());
                    meta = item.getItemMeta();
                    meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "No Previous Page");
                }
            } else if (j == 9 * Main.getInstance().getRows() + 3) {
                item = new ItemStack(Material.BARRIER, 1);
                meta = item.getItemMeta();
                meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "TRASH");
            } else if (j == 9 * Main.getInstance().getRows() + 4) {
                item = new ItemStack(Material.HOPPER, 1);
                meta = item.getItemMeta();
                switch (Main.getInstance().getSettingsMap().get(uuid).getAutoPickup()) {
                    case 0:
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: Disabled");
                        break;
                    case 1:
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: All Items");
                        break;
                    case 2:
                        meta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + "Auto Pickup: On Full Inventory");
                        break;
                }
            } else if (j == 9 * Main.getInstance().getRows() + 5) {
                item = new ItemStack(Material.TRAPPED_CHEST, 1);
                meta = item.getItemMeta();
                meta.setDisplayName("" + ChatColor.GOLD + ChatColor.BOLD + "Chest");
                if (Main.getInstance().isChestWithdraw()) {
                    lore.add(ChatColor.BLUE + "Click to withdraw chest");
                }
                if (Main.getInstance().isHopperTransfer()) {
                    lore.add(ChatColor.BLUE + "Shift click to get hopper transfer block");
                }
            } else if (j > 9 * Main.getInstance().getRows() + 5) {
                if (i < Main.getInstance().getSettingsMap().get(uuid).getMax()) {
                    item = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.LIME.getData());
                    meta = item.getItemMeta();
                    meta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + "Next Page");
                } else {
                    item = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getData());
                    meta = item.getItemMeta();
                    meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "No Next Page");
                }
            }
            if (meta != null) {
                lore.addAll(Main.getInstance().getIdentifier());
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            chest.setItem(j, item);
        }
        chests.put(i, chest);
        setEmptyTrash(uuid, name);
        Main.getInstance().getChestsMap().put(uuid, chests);
    }

    public void setEmptyTrash(UUID uuid, String name) {
        Inventory trash = Bukkit.createInventory(null, 54, name + "'s Trash");
        ItemStack exitTrashItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getData());
        ItemMeta exitTrashMeta = exitTrashItem.getItemMeta();
        exitTrashMeta.setLore(Main.getInstance().getIdentifier());
        exitTrashMeta.setDisplayName(ChatColor.BOLD + "Exit Trash");
        exitTrashItem.setItemMeta(exitTrashMeta);
        ItemStack emptyTrashItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta emptyTrashMeta = emptyTrashItem.getItemMeta();
        emptyTrashMeta.setLore(Main.getInstance().getIdentifier());
        emptyTrashMeta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "Empty Trash");
        emptyTrashItem.setItemMeta(emptyTrashMeta);
        for (int i = 0; i < 6; i++) {
            int j = i * 9;
            trash.setItem(j, exitTrashItem);
            trash.setItem(j + 8, emptyTrashItem);
        }
        Main.getInstance().getTrashMap().put(uuid, trash);
    }
}
