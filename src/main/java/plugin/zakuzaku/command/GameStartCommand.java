package plugin.zakuzaku.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.zakuzaku.Main;
import plugin.zakuzaku.data.PlayerScore;

public class GameStartCommand extends BaseCommand implements Listener {

  private Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Material> allowedBlocks = new ArrayList<>();
  private List<Location> generatedBlocks = new ArrayList<>();
  private List<Material> blocksList;


  public GameStartCommand(Main main) {
    this.main = main;
    allowedBlocks.addAll(List.of(Material.STONE, Material.BLACKSTONE,
        Material.DIAMOND_ORE,
        Material.LAPIS_ORE,
        Material.IRON_ORE));
  }

  @Override
  public boolean onExecutePlayerCommand(Player player) {
    PlayerScore nowPlayer = getPlayerScore(player);
    nowPlayer.setGameTime(40);
    World world = player.getWorld();

    initPlayerStatus(player);

    player.sendTitle("採掘ゲームスタート!", "", 10, 40, 10);

    Bukkit.getScheduler().runTaskLater(main, () -> {
      player.sendTitle("お疲れさまでした!",
          nowPlayer.getPlayerName() + "合計" + nowPlayer.getScore() + "点でした。",
          0, 60, 0);
      nowPlayer.setScore(0);

      removeGeneratedBlocks();
    }, 30 * 20);

    List<Material> blocksList = getMaterials();

    //ランダムに並べたMaterialの出現場所
    generateRandomBlocks(player, world, blocksList);
    // ここでallowedBlocksリストを生成したブロックの種類のみにリセットする
    allowedBlocks.clear();
    allowedBlocks.addAll(blocksList);
    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender) {
    return false;
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

    // allowedBlocks リストに含まれない場合は採掘をキャンセル
    //元からワールドにあるものも含むとバグになる可能性もある
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
      return;
    }

    // 壊れたブロックが生成されたブロックかどうかをチェックします
    // コマンド実行時に生成されていないブロックのポイントを加算しない。
    Location blockLocation = e.getBlock().getLocation();
    if (!generatedBlocks.contains(blockLocation)) {
      return;
    }

    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
        if (allowedBlocks.contains(brokenBlockType)) {
          playerBlockScore(player, playerScore, brokenBlockType);
        }
      }
    }
  }


  /**
   * コマンドを実行したプレイヤーの初期状態を FoodLevelを20、Healthレベルを20、ダイヤモンドのピッケルを装備する。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private static void initPlayerStatus(Player player) {
    player.setFoodLevel(20);
    player.setHealth(20);
    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
  }

  /**
   * 生成するブロックの出現場所
   *
   * @param player     コマンドを実行したプレイヤー
   * @param world      コマンドを実行したプレイーのワールド情報
   * @param blocksList allowedBlockリスト内に指定したブロック
   */
  private void generateRandomBlocks(Player player, World world, List<Material> blocksList) {
    int count = 0;
    Location playerLocation = player.getLocation();
    //プレイヤーの周囲にランダムにブロックを出現させる座標
    for (int x = playerLocation.getBlockX() - 4;
        x <= playerLocation.getBlockX() + 4; x++) {
      for (int y = playerLocation.getBlockY() - 3;
          y <= playerLocation.getBlockY() + 3; y++) {
        for (int z = playerLocation.getBlockZ() - 4;
            z <= playerLocation.getBlockZ() + 4; z++) {

          Location blockLocation = new Location(world, x, y, z);
          //プレイヤーから一定距離以上離れた位置に生成
          if (blockLocation.distance(playerLocation) > 3) {
            //空気ブロックの上にのみ生成
            if (!blockLocation.getBlock().getType().isSolid()) {
              //生成ブロックの数が上限に達したら終了
              if (count >= 80) {
                break;
              }
              Material blockType = blocksList.get(count);
              if (allowedBlocks.contains(blockType)) { // allowedBlocks リストに含まれる場合のみ生成
                world.getBlockAt(x, y, z).setType(blockType);
                count++;
                //生成したブロックの位置情報をリストに追加する
                generatedBlocks.add(blockLocation);
              }
            }
          }
        }
      }
    }
  }

  /**
   * コマンド実行しているプレイヤーのスコア情報を取得している。
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
   * @param player コマンドを実行したプレイヤー。
   * @return 新規プレイヤー。
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
  }

  /**
   * 生成されるBlockのList
   *
   * @return Blockのscoreと生成される数
   */
  private static List<Material> getMaterials() {
    //Listに持たせたMaterialの任意の数を指定。。
    List<Material> blocksList = new ArrayList<>();
    blocksList.addAll(Collections.nCopies(50, Material.STONE));
    blocksList.addAll(Collections.nCopies(2, Material.DIAMOND_ORE));
    blocksList.addAll(Collections.nCopies(2, Material.LAPIS_ORE));
    blocksList.addAll(Collections.nCopies(20, Material.BLACKSTONE));
    blocksList.addAll(Collections.nCopies(20, Material.IRON_ORE));
    Collections.shuffle(blocksList);
    return blocksList;
  }

  /**
   * それぞれのブロックにscoreを設定した。
   *
   * @param player          コマンドを実行したプレイヤー。
   * @param playerScore     それぞれのブロックに設定したスコア。
   * @param brokenBlockType 採掘したブロックの種類。
   */
  private static void playerBlockScore(Player player, PlayerScore playerScore,
      Material brokenBlockType) {
    int point = switch (brokenBlockType) {
      case STONE -> 5;
      case IRON_ORE -> 20;
      case BLACKSTONE -> 15;
      case DIAMOND_ORE -> 100;
      case LAPIS_ORE -> 75;
      default -> 0;
    };
    upDatePlayerScore(player, playerScore, brokenBlockType, point);
  }

  /**
   * 3回連続で同じブロックを採掘するとscoreが3倍になる。
   *
   * @param player          コマンド実行したプレイヤー
   * @param playerScore     採掘したブロック毎のscore
   * @param brokenBlockType Listに登録されているBlock
   * @param point           同じブロックを３回連続で採掘すると４回目以降はscoreが３倍になる。
   */
  private static void upDatePlayerScore(Player player, PlayerScore playerScore,
      Material brokenBlockType,
      int point) {
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

  /**
   * 生成したブロック情報を取得しゲーム終了時に削除する。
   */
  private void removeGeneratedBlocks() {
    for (Location blockLocation : generatedBlocks) {
      blockLocation.getBlock().setType(Material.AIR); // ブロックを空気に変更
    }
    generatedBlocks.clear(); // リストをクリア
  }
}