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
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Material> allowedBlocks = new ArrayList<>();
  private List<Location> generatedBlocks = new ArrayList<>();

  public GameStartCommand(Main main) {
    this.main = main;
    allowedBlocks.addAll(List.of(Material.STONE, Material.BLACKSTONE,
        Material.DIAMOND_ORE,
        Material.LAPIS_ORE));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      PlayerScore nowPlayer = getPlayerScore(player);
      nowPlayer.setGameTime(20);
      World world = player.getWorld();

      PlayerInventory inventory = player.getInventory();
      inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

      Bukkit.getScheduler().runTaskLater(main, () -> {
        player.sendTitle("ゲームが終了しました!",
            nowPlayer.getPlayerName() + "合計" + nowPlayer.getScore() + "点でした。",
            0, 45, 0);
        nowPlayer.setScore(0);

        removeGeneratedBlocks();
      }, 15 * 20);

      //Listに持たせたMaterialの任意の数を指定。。
      List<Material> blocksList = new ArrayList<>();
      blocksList.addAll(Collections.nCopies(40, Material.STONE));
      blocksList.addAll(Collections.nCopies(3, Material.DIAMOND_ORE));
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
                if (count >= 80) {
                  break;
                }
                Material blockType = blocksList.get(count);
                world.getBlockAt(x, y, z).setType(blockType);
                count++;
                //生成したブロックの位置情報をリストに追加する
                generatedBlocks.add(blockLocation);
              }
            }
          }
        }
      }
      // ここでallowedBlocksリストを生成したブロックの種類のみにリセットする
      allowedBlocks.clear();
      allowedBlocks.addAll(blocksList);
    }
    return true;
  }


  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    // ゲームが終了している場合は点数加算しない
    PlayerScore nowPlayer = getPlayerScore(player);
    if (nowPlayer.getGameTime() <= 0) {
      return;
    }

    // 壊れたブロックが生成されたブロックかどうかをチェックします
    Location blockLocation = e.getBlock().getLocation();
    if (!generatedBlocks.contains(blockLocation)) {
      return; //コマンド実行時に生成されていないブロックのポイントを無視します
    }

    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
        Material brokenBlockType = e.getBlock().getType();
        if (allowedBlocks.contains(brokenBlockType)) {

          int point = switch (brokenBlockType) {
            case STONE -> 10;
            case BLACKSTONE -> 15;
            case DIAMOND_ORE -> 100;
            case LAPIS_ORE -> 90;
            default -> 0;
          };

          if (brokenBlockType == playerScore.getLastMinedBlock()) {
            playerScore.incrementConsecutiveBlocksMined();
            if (playerScore.getConsecutiveBlocksMined() >= 3) {
              playerScore.setScore(playerScore.getScore() + point * 3); // 3回目以降は2倍の点数
            } else {
              playerScore.setScore(playerScore.getScore() + point);
            }
          } else {
            playerScore.resetConsecutiveBlocksMined();
            playerScore.setScore(playerScore.getScore() + point);
          }
          playerScore.setLastMinedBlock(brokenBlockType);
          player.sendMessage("採掘しました！現在のスコアは" + playerScore.getScore() + "点です。");
        }
      }
    }

    //gameStart実行時に生成されたブロック以外は採掘できなくなる
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
    }
  }


  /**
   * 現在実行しているプレイヤーのスコア情報を取得している。
   *
   * @param player コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    if (playerScoreList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for (PlayerScore playerScore : playerScoreList) {
        if (!playerScore.getPlayerName().equals(player.getName())) {
          return addNewPlayer(player);
        } else {
          return playerScore;
        }
      }
    }
    return null;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   *
   * @param player コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
  }

  /**
   * 生成したブロック情報からブロックを削除する
   */
  private void removeGeneratedBlocks() {
    for (Location blockLocation : generatedBlocks) {
      blockLocation.getBlock().setType(Material.AIR); // ブロックを空気に変更
    }
    generatedBlocks.clear(); // リストをクリア
  }
}