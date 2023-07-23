package plugin.zakuzaku.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class gameStartCommand implements CommandExecutor {

  private final int stackAmount = 27;
  private Block playerBlock;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      World world = player.getWorld();

      List<Material> blocksList = new ArrayList<>();
      blocksList.add(Material.STONE);
      blocksList.add(Material.DIAMOND_ORE);
      blocksList.add(Material.LAPIS_ORE);

      int random = ThreadLocalRandom.current().nextInt(blocksList.size());

      for (int i = 0; i < 52; i++) {
        blocksList.add(Material.STONE);
      }
      for (int i = 0; i < 10; i++) {
        blocksList.add(Material.DIAMOND_ORE);
      }
      for (int i = 0; i < 10; i++) {
        blocksList.add(Material.LAPIS_ORE);
      }

      Collections.shuffle(blocksList, ThreadLocalRandom.current());

      int count = 0;
      playerBlock = player.getLocation().subtract(10, 0, 0).getBlock();

      playerBlock = player.getLocation().getBlock();
      for (int x = playerBlock.getX() - 6; x <= playerBlock.getX(); x++) {
        for (int y = playerBlock.getY(); y < playerBlock.getY() + 3; y++) {
          for (int z = playerBlock.getZ() - 6; z < playerBlock.getZ(); z++) {
            if (count >= 72) {
              break;
            }

            Material blockType = blocksList.get(count);
            world.getBlockAt(x, y, z).setType(blockType);
            count++;
          }
        }
      }
    }
    return true;
  }
}
