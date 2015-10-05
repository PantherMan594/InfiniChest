/*
 * Copyright (c) 2015 David Shen. All Rights Reserved.
 * Created by PantherMan594.
 */

package net.cubexmc.infinichest.Utils;

import net.cubexmc.infinichest.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
public class Settings {
    private String name;
    private UUID uuid;
    private Integer autoPickup = 0;
    private Integer lastPage = 1;
    private HashMap<Integer, ItemStack[]> items = new HashMap<>();
    private Integer max = 1;

    public static Settings load(UUID uuid) {
        boolean isNew = false;
        if (!Main.plugin.getDataFolder().exists()) {
            if (!Main.plugin.getDataFolder().mkdir()) {
                Bukkit.getLogger().warning("Unable to create config folder!");
            }
        }
        if (!new File(Main.plugin.getDataFolder() + File.separator + "playedata").exists()) {
            if (!new File(Main.plugin.getDataFolder() + File.separator + "playedata").mkdir()) {
                Bukkit.getLogger().warning("Unable to create playerdata folder!");
            }
        }
        File f = new File(Main.plugin.getDataFolder() + File.separator + "playedata" + File.separator + uuid.toString() + ".yml");
        try {
            isNew = f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Settings settings = new Settings();
        settings.setId(uuid);
        settings.max(uuid);
        if (!isNew) {
            FileConfiguration con = YamlConfiguration.loadConfiguration(f);
            settings.setName(con.getString("settings.lastname"));
            settings.setAutoPickup(con.getInt("settings.autopickup"));
            settings.setLastPage(con.getInt("settings.lastpage"));
            for (int i = 1; i < 1001; i++) {
                List<?> itemList = con.getList("items." + i);
                if (itemList != null && !itemList.isEmpty()) {
                    ItemStack[] inv = itemList.toArray(new ItemStack[itemList.size()]);
                    settings.setItems(inv, i);
                }
            }
        }
        return settings;
    }

    public static void save(Settings settings) {
        File f = new File(Main.plugin.getDataFolder() + File.separator + "playedata" + File.separator + settings.getUniqueId() + ".yml");
        FileConfiguration con = YamlConfiguration.loadConfiguration(f);
        con.set("settings.lastname", Bukkit.getPlayer(settings.getUniqueId()).getName());
        con.set("settings.autopickup", settings.getAutoPickup());
        con.set("settings.lastpage", settings.getLastPage());
        for (int i = 1; i < 1001; i++) {
            if (Main.chestsMap.containsKey(settings.getUniqueId()) && Main.chestsMap.get(settings.getUniqueId()).containsKey(i)) {
                ItemStack[] items = Main.chestsMap.get(settings.getUniqueId()).get(i).getContents();
                boolean empty = true;
                for (int j = 0; j < 45 && empty; j++) {
                    if (items[j] != null) {
                        empty = false;
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

    public Settings max(UUID uuid) {
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
        this.max = 1;
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

}