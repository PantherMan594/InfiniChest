/*
 * Copyright (c) 2016 David Shen. All Rights Reserved.
 * Created by PantherMan594.
 */

package com.pantherman594.infinichest.Utils;

import com.pantherman594.infinichest.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by David on 10/03.
 *
 * @author David
 */
@SuppressWarnings({"SuspiciousToArrayCall", "WeakerAccess"})
public class Settings {
    private String name;
    private UUID uuid;
    private Integer autoPickup = 0;
    private Integer lastPage = 1;
    private HashMap<Integer, ItemStack[]> items = new HashMap<>();
    private Integer max = 1;
    private BukkitTask r;

    public Settings(final UUID uuid) {
        boolean isNew = false;
        if (!Main.getInstance().getDataFolder().exists()) {
            if (!Main.getInstance().getDataFolder().mkdir()) {
                Bukkit.getLogger().warning("Unable to create config folder!");
            }
        }
        if (!new File(Main.getInstance().getDataFolder() + File.separator + "playerdata").exists()) {
            if (!new File(Main.getInstance().getDataFolder() + File.separator + "playerdata").mkdir()) {
                Bukkit.getLogger().warning("Unable to create playerdata folder!");
            }
        }
        File f = new File(Main.getInstance().getDataFolder() + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
        try {
            isNew = f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setId(uuid);
        setMax(uuid);
        if (!isNew) {
            FileConfiguration con = YamlConfiguration.loadConfiguration(f);
            setName(con.getString("settings.lastname"));
            setAutoPickup(con.getInt("settings.autopickup"));
            setLastPage(con.getInt("settings.lastpage"));
            for (int i = 1; i <= getMax(); i++) {
                List<?> itemList = con.getList("items." + i);
                if (itemList != null && !itemList.isEmpty()) {
                    ItemStack[] inv = itemList.toArray(new ItemStack[itemList.size()]);
                    setItems(inv, i);
                }
            }
        }
        r = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::save, (long) (Main.getInstance().getSaveInterval() * 20), (long) (Main.getInstance().getSaveInterval() * 20));
    }

    public void save() {
        r.cancel();
        File f = new File(Main.getInstance().getDataFolder() + File.separator + "playerdata" + File.separator + getUniqueId() + ".yml");
        FileConfiguration con = YamlConfiguration.loadConfiguration(f);
        if (Bukkit.getPlayer(getUniqueId()) != null) {
            con.set("settings.lastname", Bukkit.getPlayer(getUniqueId()).getName());
        } else {
            con.set("settings.lastname", getName());
        }
        con.set("settings.autopickup", getAutoPickup());
        con.set("settings.lastpage", getLastPage());
        for (int i = 1; i <= getMax(); i++) {
            if (Main.getInstance().getChestsMap().containsKey(getUniqueId()) && Main.getInstance().getChestsMap().get(getUniqueId()).containsKey(i)) {
                ItemStack[] items = Main.getInstance().getChestsMap().get(getUniqueId()).get(i).getContents();
                boolean empty = true;
                for (int j = 0; j < 9 * Main.getInstance().getRows() + 9; j++) {
                    if (items[j] != null) {
                        if (!items[j].getType().equals(Material.CHEST) && items[j].getItemMeta().getLore() != null && items[j].getItemMeta().getLore().contains(Main.getInstance().getIdentifier().get(0))) {
                            items[j] = null;
                        } else {
                            empty = false;
                        }
                    }
                }
                if (!empty) {
                    con.set("items." + i, items);
                } else {
                    con.set("items." + i, null);
                }
            }
        }
        try {
            con.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Settings setId(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public Settings setItems(ItemStack[] items, Integer id) {
        this.items.put(id, items);
        return this;
    }

    public String getName() {
        return name;
    }

    public Settings setName(String name) {
        this.name = name;
        return this;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Integer getAutoPickup() {
        return autoPickup;
    }

    public Settings setAutoPickup(Integer autoPickup) {
        // 0: No auto pickup
        // 1: Auto pickup always
        // 2: Auto pickup with inv full
        this.autoPickup = autoPickup;
        return this;
    }

    public Integer getLastPage() {
        return lastPage;
    }

    public Settings setLastPage(Integer lastPage) {
        this.lastPage = lastPage;
        return this;
    }

    public HashMap<Integer, ItemStack[]> getItems() {
        return items;
    }

    public Integer getMax() {
        return max;
    }

    public Settings setMax(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) {
            this.max = 1000;
            return this;
        }
        for (int i = 0; i < 1000; i++) {
            int j = 1000 - i;
            if (p.hasPermission("InfiniChest.size." + j)) {
                this.max = j;
                return this;
            }
        }
        this.max = 0;
        return this;
    }

}
