package org.seworl.doggy.Events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ControlDog implements Listener {


    public static final double                offset        = 0.5;
    private final       Map<UUID, Boolean>    isSitMap      = new HashMap<>();
    private final       Map<UUID, Set<Chunk>> ownerChunkMap = new HashMap<>();


    @EventHandler(ignoreCancelled = true) public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking() &&
            ((Entity) player).isOnGround() &&
            player.getInventory().getItemInMainHand().getType().equals(Material.BONE)) {

            UUID       playerUUID = player.getUniqueId();
            boolean    isSit      = isSitMap.getOrDefault(playerUUID, true);
            AtomicLong cost       = new AtomicLong();
            long       count;

            ownerChunkMap.getOrDefault(player.getUniqueId(), new HashSet<>()).forEach(chunk -> {
                chunk.load();
                cost.addAndGet(Arrays.stream(chunk.getEntities())
                                     .filter(entity -> entity instanceof Wolf wolf &&
                                                       wolf.isTamed() &&
                                                       Objects.equals(wolf.getOwner(), player))
                                     .count());
            });

            cost.updateAndGet(operand -> operand / 9);

            if (player.getInventory().containsAtLeast(new ItemStack(Material.BONE), cost.intValue())) {

                player.getInventory().removeItem(new ItemStack(Material.BONE, cost.intValue()));

                castMagicEffect(player, isSit);

                count = toggleWolvesSit(event, player, isSit);

                isSitMap.put(playerUUID, !isSit);
                if (isSit) {
                    player.sendActionBar(Component.text(String.format("%s Wolves Sat", count)).color(NamedTextColor.GRAY));
                } else {
                    ownerChunkMap.remove(playerUUID);
                }
            }
        }
    }

    private static long toggleWolvesSit(PlayerToggleSneakEvent event, Player player, boolean isSit) {
        List<Wolf> wolves = player.getWorld()
                                  .getEntitiesByClass(Wolf.class)
                                  .stream()
                                  .filter(wolf -> wolf.isTamed() && Objects.equals(wolf.getOwner(), event.getPlayer()))
                                  .toList();
        wolves.forEach(wolf -> {
            wolf.setSitting(isSit);
            gatherWolfIfRemoted(player, isSit, wolf);
        });
        return wolves.size();
    }

    private static void gatherWolfIfRemoted(Player player, boolean isSit, Wolf wolf) {
        if (wolf.getLocation().distance(player.getLocation()) > 12 && !isSit) {
            wolf.teleport(getproperLocation(player));
        }
    }

    private static @NotNull Location getproperLocation(Player player) {
        Location location = getRandomLocation(player);

        while (!isProper(player, location)) {
            location = getRandomLocation(player);
        }

        return location;
    }

    private static boolean isProper(Player player, Location location) {
        return player.getWorld().getBlockAt(location.add(-offset, 0, 0)).isEmpty() &&
               player.getWorld().getBlockAt(location.add(offset, 0, 0)).isEmpty() &&
               player.getWorld().getBlockAt(location.add(0, 0, -offset)).isEmpty() &&
               player.getWorld().getBlockAt(location.add(0, 0, offset)).isEmpty();
    }

    private static @NotNull Location getRandomLocation(Player player) {
        return player.getLocation()
                     .add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2)
                     .toHighestLocation()
                     .add(0, 1, 0);
    }

    private static void castMagicEffect(Player player, boolean isSit) {
        player.spawnParticle(Particle.ENCHANT, player.getLocation(), 500, 2, 0.5, 2, 0.1);
        player.spawnParticle(Particle.WITCH, player.getLocation(), 100, 0.5, 0.25, 0.5, 0.1);
        player.playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 2);
        player.playSound(player, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, isSit ? 1.8F : 2);
        player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 0);
    }


    @EventHandler(ignoreCancelled = true) public void onEntitiesUnload(EntitiesUnloadEvent event) {
        Chunk chunk = event.getChunk();

        event.getEntities().forEach(entity -> {
            if (entity instanceof Wolf wolf && wolf.getOwner() != null) {
                UUID ownerUUID = wolf.getOwnerUniqueId();
                ownerChunkMap.computeIfAbsent(ownerUUID, k -> new HashSet<>()).add(chunk);
            }
        });
    }


}