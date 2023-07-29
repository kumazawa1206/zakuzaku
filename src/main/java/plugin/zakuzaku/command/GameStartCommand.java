package plugin.zakuzaku.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

public class GameStartCommand implements CommandExecutor, Listener {

  private Block playerBlock;
  private List<Material> allowedBlocks;
  private Player player;
  private int score;

  public GameStartCommand() {
    allowedBlocks = List.of(Material.REDSTONE, Material.BLACKSTONE,
        Material.DIAMOND_ORE,
        Material.LAPIS_ORE);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      this.player = player;
      World world = player.getWorld();
      PlayerInventory inventory = player.getInventory();
      inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

      //Listに持たせたMaterialをランダムに並べる。
      List<Material> blocksList = new ArrayList<>();
      blocksList.addAll(Collections.nCopies(40, Material.STONE));
      blocksList.addAll(Collections.nCopies(3, Material.DIAMOND_ORE));
      blocksList.addAll(Collections.nCopies(40, Material.STONE));
      blocksList.addAll(Collections.nCopies(3, Material.LAPIS_ORE));
      blocksList.addAll(Collections.nCopies(40, Material.BLACKSTONE));
      Collections.shuffle(blocksList);

      int count = 0;
      Location playerLocation = player.getLocation();
      int offsetX = 4;
      int offsetY = 3;
      int offsetZ = 4;
      int distance = 3;

      for (int x = playerLocation.getBlockX() - offsetX; x <= playerLocation.getBlockX() + offsetX;
          x++) {
        for (int y = playerLocation.getBlockY() - offsetY;
            y <= playerLocation.getBlockY() + offsetY; y++) {
          for (int z = playerLocation.getBlockZ() - offsetZ;
              z <= playerLocation.getBlockZ() + offsetZ; z++) {

            Location blockLocation = new Location(world, x, y, z);
            if (blockLocation.distance(playerLocation) > distance) {
              if (!blockLocation.getBlock().getType().isSolid()) {
                if (count >= 126) {
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
    }
    return true;
  }

  private Material getRandomBlockType() {
    Material[] blockTypes = {Material.DIAMOND_ORE, Material.STONE, Material.LAPIS_ORE};
    int random = new SplittableRandom().nextInt(blockTypes.length);
    return blockTypes[random];
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    if (Objects.isNull(player)) {
      return;
    }
    if (Objects.isNull(this.player)) {
      return;
    }

    if (this.player.getName().equals(player.getName())) {
      score += 10;
      player.sendMessage("採掘しました！現在のスコアは" + score + "点です。");
    }
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
    }

  }
}

