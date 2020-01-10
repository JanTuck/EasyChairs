package net.pl3x.bukkit.easychairs;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
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

                // get location and direction
                BlockFace facing = ((Directional) block.getBlockData()).getFacing();
                Location loc = block.getLocation();

                // setup location
                double x = loc.getX() + 0.5D;
                double y = loc.getY() + 0.3D;
                double z = loc.getZ() + 0.5D;

                // fix the seat offset
                double offset = 0.25D;
                if (facing == BlockFace.NORTH) z += offset;
                else if (facing == BlockFace.EAST) x -= offset;
                else if (facing == BlockFace.SOUTH) z -= offset;
                else if (facing == BlockFace.WEST) x += offset;

                // setup armor stand location with rotation based on direction
                float yaw = Location.normalizeYaw(facing.ordinal() * 90);
                Location rotatedLocation = new Location(loc.getWorld(), x, y, z, yaw, 0);

                // spawn armor stand with fully adjusted location
                ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(rotatedLocation, EntityType.ARMOR_STAND);

                // verify armor stand actually spawned
                if (!armorStand.isValid()) return;

                // finish setting up rest of the armorstand properties
                armorStand.setGravity(false);
                armorStand.setCanTick(false);
                armorStand.setCanMove(false);
                armorStand.setVisible(false);
                armorStand.setMarker(true);
                armorStand.setSmall(true);
                armorStand.setCustomNameVisible(false);
                armorStand.setCustomName("EasyChairs");

                // teleport player closer to chair (with proper rotation)
                player.teleport(armorStand.getLocation());

                // sit on chair
                if (!armorStand.addPassenger(player)) {
                    armorStand.remove(); // was unable to mount
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onChairEject(EntityDismountEvent event) {
                if (event.getEntityType() == EntityType.PLAYER) {
                    Entity vehicle = event.getDismounted();
                    if (isChair(vehicle)) {
                        vehicle.remove();
                    }
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
        // remove all chairs from world all worlds
        Bukkit.getWorlds().forEach(world ->
                world.getEntities().stream()
                        .filter(EasyChairs::isChair)
                        .forEach(Entity::remove));
    }

    public static boolean isChair(Entity entity) {
        if (entity == null || entity.getType() != EntityType.ARMOR_STAND) {
            return false; // not an armorstand
        }
        if (entity.isCustomNameVisible()) {
            return false; // just checking in case player set name with nametag
        }
        String name = entity.getCustomName();
        return name != null && name.equals("EasyChairs");
    }

    public static boolean isStair(Block block) {
        if (block == null || !Tag.STAIRS.isTagged(block.getType())) {
            return false; // not a stairs block
        }
        Stairs stairs = (Stairs) block.getBlockData();
        if (stairs.getShape() != Stairs.Shape.STRAIGHT) {
            return false; // ignore corner stairs
        }
        return stairs.getHalf() == Bisected.Half.BOTTOM;
    }
}
