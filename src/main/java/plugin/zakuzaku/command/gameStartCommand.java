package plugin.zakuzaku.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class gameStartCommand implements CommandExecutor, Listener {

  private Block playerBlock;
  private Set<Material> allowedBlocks;

  public gameStartCommand() {
    allowedBlocks = new HashSet<>();
    allowedBlocks.add(Material.STONE);
    allowedBlocks.add(Material.DIAMOND_ORE);
    allowedBlocks.add(Material.LAPIS_ORE);

  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      World world = player.getWorld();
      PlayerInventory inventory = player.getInventory();
      inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

      List<Material> blocksList = new ArrayList<>();
      blocksList.add(Material.STONE);
      blocksList.add(Material.DIAMOND_ORE);
      blocksList.add(Material.LAPIS_ORE);

      int random = new SplittableRandom().nextInt(blocksList.size());

      for (int i = 0; i < 3; i++) {
        blocksList.add(Material.DIAMOND_ORE);
      }
      for (int i = 0; i < 120; i++) {
        blocksList.add(Material.STONE);
      }
      for (int i = 0; i < 2; i++) {
        blocksList.add(Material.LAPIS_ORE);
      }

      Collections.shuffle(blocksList);

      int count = 0;
      Location playerLocation = player.getLocation();
      int offsetX = 4;
      int offsetY = 2;
      int offsetZ = 4;
      int distance = 2;

      for (int x = playerLocation.getBlockX() - offsetX; x <= playerLocation.getBlockX() + offsetX;
          x++) {
        for (int y = playerLocation.getBlockY() - offsetY;
            y <= playerLocation.getBlockY() + offsetY; y++) {
          for (int z = playerLocation.getBlockZ() - offsetZ;
              z <= playerLocation.getBlockZ() + offsetZ; z++) {

            Location blockLocation = new Location(world, x, y, z);
            if (blockLocation.distance(playerLocation) > distance) {
              if (!blockLocation.getBlock().getType().isSolid()) {
                if (count >= 125) {
                  break;
                }
                Material blockType = blocksList.get(count);
                world.getBlockAt(x, y, z).setType(blockType);
                count++;
              }
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
    }
  }
}
