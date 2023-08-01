package plugin.zakuzaku.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.zakuzaku.Main;
import plugin.zakuzaku.data.PlayerScore;

public class GameStartCommand implements CommandExecutor, Listener {

  private Main main;
  private List<Material> allowedBlocks;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private int gameTime = 15;

  public GameStartCommand(Main main) {
    this.main = main;

    allowedBlocks = List.of(Material.STONE, Material.BLACKSTONE,
        Material.DIAMOND_ORE,
        Material.LAPIS_ORE);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    if (sender instanceof Player player) {
      if (playerScoreList.isEmpty()) {
        addNewPlayer(player);
      } else {
        for (PlayerScore playerScore : playerScoreList) {
          if (!playerScore.getPlayerName().equals(player.getName())) {
          }
        }
      }

      gameTime = 15;
      World world = player.getWorld();
      PlayerInventory inventory = player.getInventory();
      inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

      Bukkit.getScheduler().runTaskLater(main, () -> {
        player.sendMessage("ゲーム終了！お疲れさまでした。");
      }, 15 * 20);

      //Listに持たせたMaterialの任意の数を指定。。
      List<Material> blocksList = new ArrayList<>();
      blocksList.addAll(Collections.nCopies(40, Material.STONE));
      blocksList.addAll(Collections.nCopies(3, Material.DIAMOND_ORE));
      blocksList.addAll(Collections.nCopies(40, Material.STONE));
      blocksList.addAll(Collections.nCopies(3, Material.LAPIS_ORE));
      blocksList.addAll(Collections.nCopies(40, Material.BLACKSTONE));
      Collections.shuffle(blocksList);

      //ランダムに並べたMaterialの出現場所
      int count = 0;
      Location playerLocation = player.getLocation();
      //ランダムに並べたMaterialの出現場所の座標
      for (int x = playerLocation.getBlockX() - 4;
          x <= playerLocation.getBlockX() + 4; x++) {
        for (int y = playerLocation.getBlockY() - 3;
            y <= playerLocation.getBlockY() + 3; y++) {
          for (int z = playerLocation.getBlockZ() - 4;
              z <= playerLocation.getBlockZ() + 4; z++) {

            Location blockLocation = new Location(world, x, y, z);
            if (blockLocation.distance(playerLocation) > 3) {
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

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
        playerScore.setScore(playerScore.getScore() + 10);
        player.sendMessage("採掘しました！現在のスコアは" + playerScore.getScore() + "点です。");
      }
    }
    //gameStart実行時に生成されたブロック以外は採掘できなくなる
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
    }
  }


  /**
   * 新規のプレイヤー情報をリストに追加します。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
  }
}