package com.rivalsmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class RivalSMP extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey weaponKey;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        weaponKey = new NamespacedKey(this, "weapon_id");
        Objects.requireNonNull(getCommand("rivalsmp")).setExecutor(this);
        Objects.requireNonNull(getCommand("rivalsmp")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RivalSMP enabled. This build targets the Paper 1.21.1 API.");
    }

    @Override public void onDisable() { hitCounters.clear(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color("&cRivalSMP &7v" + getDescription().getVersion()));
            sender.sendMessage(color("&e/rivalsmp give <player> <weapon>"));
            sender.sendMessage(color("&e/rivalsmp list"));
            sender.sendMessage(color("&e/rivalsmp reload"));\n            sender.sendMessage(color("&e/rivalsmp announce <message>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&cWeapons: &fBloodSword, DoomsHammer, Scythe, KillerKnife, AutoBow, CrossBow, BanHammer"));
            return true;
        }
        if (args[0].equalsIgnoreCase("announce")) {
            if (!sender.hasPermission("rivalsmp.admin")) return noPerm(sender);
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /rivalsmp announce <message>"));
                return true;
            }
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            broadcastAnimatedAnnouncement(message);
            sender.sendMessage(color("&aAnnouncement sent."));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rivalsmp.admin")) return noPerm(sender);
            reloadConfig(); sender.sendMessage(color("&aRivalSMP configuration reloaded.")); return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("rivalsmp.admin")) return noPerm(sender);
            if (args.length < 3) { sender.sendMessage(color("&cUsage: /rivalsmp give <player> <weapon>")); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            String id = normalize(args[2]);
            if (target == null) { sender.sendMessage(color("&cPlayer is not online.")); return true; }
            ItemStack item = createWeapon(id);
            if (item == null) { sender.sendMessage(color("&cUnknown weapon. Use /rivalsmp list.")); return true; }
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
            overflow.values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
            sender.sendMessage(color("&aGave " + id + " to " + target.getName() + "."));
            return true;
        }
        sender.sendMessage(color("&cUnknown subcommand. Use /rivalsmp help."));
        return true;
    }

    private ItemStack createWeapon(String id) {
        Material material = switch (id) {
            case "blood-sword" -> Material.NETHERITE_SWORD;
            case "dooms-hammer" -> Material.MACE;
            case "scythe" -> Material.NETHERITE_HOE;
            case "killer-knife" -> Material.IRON_SWORD;
            case "auto-bow" -> Material.BOW;
            case "crossbow" -> Material.CROSSBOW;
            case "ban-hammer" -> Material.NETHERITE_AXE;
            default -> null;
        };
        if (material == null) return null;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String display = switch (id) {
            case "blood-sword" -> "&c&lBlood Sword";
            case "dooms-hammer" -> "&5&lDoomsHammer";
            case "scythe" -> "&d&lScythe";
            case "killer-knife" -> "&4&lKillerKnife";
            case "auto-bow" -> "&6&lAutoBow";
            case "crossbow" -> "&b&lCrossBow";
            default -> "&e&lBanHammer";
        };
        meta.setDisplayName(color(display));
        meta.setLore(List.of(color("&7A weapon forged by RivalSMP."), color("&8ID: " + id)));
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler public void onWeaponHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;
        ItemStack held = attacker.getInventory().getItemInMainHand();
        if (!held.hasItemMeta()) return;
        String id = held.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        if (id == null) return;
        if (id.equals("blood-sword")) {
            double hearts = getConfig().getDouble("weapons.blood-sword.soul-feast-hearts", 1.0);
            double max = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(max, attacker.getHealth() + hearts * 2.0));
            int strength = getConfig().getInt("weapons.blood-sword.battle-instinct-strength", 0);
            if (strength > 0) attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, strength - 1, true, false));
        } else if (id.equals("dooms-hammer")) {
            int count = hitCounters.merge(attacker.getUniqueId(), 1, Integer::sum);
            int required = Math.max(1, getConfig().getInt("weapons.dooms-hammer.hits-required", 10));
            if (count >= required) {
                hitCounters.put(attacker.getUniqueId(), 0);
                int min = getConfig().getInt("weapons.dooms-hammer.lightning-min", 3);
                int max = Math.max(min, getConfig().getInt("weapons.dooms-hammer.lightning-max", 4));
                double radius = getConfig().getDouble("weapons.dooms-hammer.lightning-radius", 6.0);
                for (int i = 0; i < min + new Random().nextInt(max - min + 1); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double distance = Math.random() * radius;
                    victim.getWorld().strikeLightningEffect(victim.getLocation().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
                }
                victim.damage(getConfig().getDouble("weapons.dooms-hammer.thunderfall-damage", 8.0), attacker);
                attacker.sendMessage(color("&5&lULTIMATE READY &7- Thunderfall activated!"));
            }
        }
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("lifesteal.enabled", true)) return;
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        AttributeInstance maxHealth = killer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        double gain = getConfig().getDouble("lifesteal.hearts-per-kill", 1.0) * 2.0;
        double cap = getConfig().getDouble("lifesteal.maximum-hearts", 20.0) * 2.0;
        maxHealth.setBaseValue(Math.min(cap, maxHealth.getBaseValue() + gain));
        double loss = getConfig().getDouble("lifesteal.hearts-lost-on-death", 1.0) * 2.0;
        AttributeInstance deadMax = dead.getAttribute(Attribute.MAX_HEALTH);
        if (deadMax != null) {
            double floor = getConfig().getDouble("lifesteal.minimum-hearts", 1.0) * 2.0;
            deadMax.setBaseValue(Math.max(floor, deadMax.getBaseValue() - loss));
        }
    }


    private void broadcastAnimatedAnnouncement(String message) {
        final String clean = ChatColor.stripColor(color(message));
        final String prefix = "✦ RIVAL SMP ✦ ";
        // The chat line uses a smooth per-character purple-to-cyan gradient.
        Component chatLine = Component.text(prefix, TextColor.color(170, 90, 255))
                .append(gradient(clean, 0, clean.length()));
        Bukkit.broadcast(chatLine);

        // Animate the on-screen title by shifting the gradient through several frames.
        final int[] frame = {0};
        final int totalFrames = 18;
        final org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            Component title = gradient("✦ RIVAL SMP ✦", frame[0], 13);
            Component subtitle = gradient(clean, frame[0] + 4, clean.length());
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(650), Duration.ofMillis(150));
            Title shown = Title.title(title, subtitle, times);
            for (Player player : Bukkit.getOnlinePlayers()) player.showTitle(shown);
            frame[0]++;
        }, 0L, 4L);
        Bukkit.getScheduler().runTaskLater(this, task::cancel, totalFrames * 4L);
    }

    private Component gradient(String text, int offset, int length) {
        if (text.isEmpty()) return Component.empty();
        int[] start = {170, 70, 255}; // violet
        int[] end = {55, 220, 255};   // cyan
        Component result = Component.empty();
        int count = text.length();
        for (int i = 0; i < count; i++) {
            double t = count <= 1 ? 0 : ((double) i / (count - 1));
            // Small phase shift makes the gradient visibly move between title frames.
            t = (t + (offset % 18) / 18.0) % 1.0;
            int r = (int) Math.round(start[0] + (end[0] - start[0]) * t);
            int g = (int) Math.round(start[1] + (end[1] - start[1]) * t);
            int b = (int) Math.round(start[2] + (end[2] - start[2]) * t);
            result = result.append(Component.text(String.valueOf(text.charAt(i)),
                    TextColor.color(r, g, b)));
        }
        return result;
    }

    private boolean noPerm(CommandSender s) { s.sendMessage(color("&cYou don't have permission.")); return true; }
    private String normalize(String input) { return input.toLowerCase(Locale.ROOT).replace("_", "-"); }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "list", "give", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return null;
        if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            return List.of("blood-sword", "dooms-hammer", "scythe", "killer-knife", "auto-bow", "crossbow", "ban-hammer");
        return Collections.emptyList();
    }
}
