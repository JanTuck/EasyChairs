package net.pl3x.bukkit.easychairs;


import net.minecraft.server.v1_14_R1.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftChatMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

public class EasyChairs extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onClickChair(PlayerInteractEvent event) {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    return; // not right clicking block
                }

                Player player = event.getPlayer();
                if (player.isSneaking()) {
                    return; // sneaking
                }

                if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    return; // not empty hand
                }

                Block block = event.getClickedBlock();
                if (!isStair(block)) {
                    return; // not a stair
                }

                final double offset = 0.25D;
                double xOffset = 0.5D;
                double zOffset = 0.5D;
                switch (((Directional) block.getBlockData()).getFacing()) {
                    case NORTH:
                        zOffset += offset;
                        break;
                    case EAST:
                        xOffset -= offset;
                        break;
                    case SOUTH:
                        zOffset -= offset;
                        break;
                    case WEST:
                        xOffset += offset;
                        break;
                }

                final float yaw = Location.normalizeYaw(((Directional) block.getBlockData()).getFacing().ordinal() * 90);

                Location loc = block.getLocation();
                loc.setYaw(yaw);
                loc.setPitch(0);

                EntityArmorStand armorstand = new EntityArmorStand(((CraftWorld) loc.getWorld()).getHandle(), loc.getX() + xOffset, loc.getY() + 0.3D, loc.getZ() + zOffset);

                armorstand.lastYaw = armorstand.yaw = yaw;
                armorstand.lastPitch = armorstand.pitch = 0;
                armorstand.setHeadRotation(yaw);

                armorstand.setNoGravity(true);
                armorstand.canTick = false;
                armorstand.canTickSetByAPI = true;
                armorstand.canMove = false;
                armorstand.setInvisible(true);
                armorstand.setMarker(true);
                armorstand.setSmall(true);
                armorstand.setCustomNameVisible(false);
                armorstand.setCustomName(CraftChatMessage.fromStringOrNull("EasyChairs"));

                armorstand.world.addEntity(armorstand);

                player.teleport(loc);

                if (armorstand.getBukkitLivingEntity().addPassenger(player)) {
                    event.setCancelled(true);
                } else {
                    armorstand.die();
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onChairEject(EntityDismountEvent event) {
                if (event.getEntityType() != EntityType.PLAYER) {
                    return; // not a player
                }
                Entity vehicle = event.getDismounted();
                if (isChair(vehicle)) {
                    vehicle.remove();
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerQuit(PlayerQuitEvent event) {
                Entity vehicle = event.getPlayer().getVehicle();
                if (isChair(vehicle)) {
                    vehicle.remove();
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onChunkLoad(ChunkLoadEvent event) {
                for (Entity entity : event.getChunk().getEntities()) {
                    if (isChair(entity)) {
                        entity.remove();
                    }
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onChunkUnload(ChunkUnloadEvent event) {
                for (Entity entity : event.getChunk().getEntities()) {
                    if (isChair(entity)) {
                        entity.remove();
                    }
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream()
                .filter(EasyChairs::isChair)
                .forEach(Entity::remove));
    }

    public static boolean isChair(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getType() != EntityType.ARMOR_STAND) {
            return false;
        }
        if (entity.isCustomNameVisible()) {
            return false;
        }
        String name = entity.getCustomName();
        return name != null && name.equals("EasyChairs");
    }

    public static boolean isStair(Block block) {
        if (block == null || !Tag.STAIRS.isTagged(block.getType())) {
            return false;
        }
        Stairs stairs = (Stairs) block.getBlockData();
        if (stairs.getShape() != Stairs.Shape.STRAIGHT) {
            return false;
        }
        return stairs.getHalf() == Bisected.Half.BOTTOM;
    }
}
