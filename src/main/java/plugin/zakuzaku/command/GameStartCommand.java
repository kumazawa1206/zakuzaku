package plugin.zakuzaku.command;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.zakuzaku.Main;
import plugin.zakuzaku.data.ExecutingPlayer;
import plugin.zakuzaku.mapper.PlayerScoreMapper;
import plugin.zakuzaku.mapper.data.PlayerScore;

/**
 * 制限時間内にランダムで出現されるブロックを採掘して、スコアを獲得するゲームです、 ブロック毎にスコア・生成数が設定されていて、スコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */
public class GameStartCommand extends BaseCommand implements Listener {

  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String LIST = "list";

  private Main main;
  private List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private List<Material> allowedBlocks = new ArrayList<>();
  private List<Location> generatedBlocks = new ArrayList<>();

  private SqlSessionFactory sqlSessionFactory;

  public final int GAME_TIME = 30 * 20;


  public GameStartCommand(Main main) {
    this.main = main;

    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    allowedBlocks.addAll(List.of(
        Material.STONE,
        Material.BLACKSTONE,
        Material.DIAMOND_ORE,
        Material.LAPIS_ORE,
        Material.IRON_ORE));
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label,
      String[] args) {
    if (args.length == 1 && LIST.equals(args[0])) {
      try (SqlSession session = sqlSessionFactory.openSession()) {
        PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
        List<PlayerScore> playerScoreList = mapper.selectList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
        for (PlayerScore playerScore : playerScoreList) {
          LocalDateTime date = LocalDateTime.parse(playerScore.getRegisteredAt(), formatter);

          player.sendMessage(
              playerScore.getId() + " | "
                  + playerScore.getPlayerName() + " | "
                  + playerScore.getScore() + " | "
                  + playerScore.getDifficulty() + " | "
                  + date.format(formatter));
        }
      }
      return false;
    }

    String difficulty = getDifficulty(player, args);
    if (difficulty.equals(NONE)) {
      return false;
    }

    ExecutingPlayer nowExecutingPlayer = getPlayerScore(player);

    initPlayerStatus(player);

    gamePlay(player, nowExecutingPlayer, difficulty);
    return true;
  }

  /**
   * 難易度をコマンド引数から取得
   *
   * @param player コマンド実行者
   * @param args   コマンド引数
   * @return 難易度
   */
  private String getDifficulty(Player player, String[] args) {
    if (args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(
        args[0]))) {
      return args[0];
    }
    player.sendMessage(
        ChatColor.RED + "実行できません。コマンド引数の１つ目に難易度設定が必要です。[easy, normal, hard]");
    return NONE;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
      String[] args) {
    return false;
  }


  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    if (Objects.isNull(player) || executingPlayerList.isEmpty()) {
      return;
    }

    // ゲームが終了している場合は点数加算しない
    ExecutingPlayer nowPlayer = getPlayerScore(player);
    if (nowPlayer.getGameTime() <= 0) {
      return;
    }

    // allowedBlocks リストに含まれない場合は採掘をキャンセル
    Material brokenBlockType = e.getBlock().getType();
    if (!allowedBlocks.contains(brokenBlockType)) {
      e.setCancelled(true);
      return;
    }

    //壊れたブロックが生成されたブロックかどうかをチェックし
    //コマンド実行時に生成されていないブロックのポイントを加算しない。
    Location blockLocation = e.getBlock().getLocation();
    if (!generatedBlocks.contains(blockLocation)) {
      return;
    }

    for (ExecutingPlayer executingPlayer : executingPlayerList) {
      if (executingPlayer.getPlayerName().equals(player.getName())
          && allowedBlocks.contains(brokenBlockType)) {
        int point = getBlockScore(brokenBlockType);

        updatePlayerScore(player, brokenBlockType, executingPlayer, point);
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
   * 新規のプレイヤー情報をリストに追加します。
   *
   * @param player コマンドを実行したプレイヤー。
   * @return 新規プレイヤー。
   */
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲームを実行します。
   *
   * @param player             コマンドを実行したプレイヤー。
   * @param nowExecutingPlayer プレイヤースコア情報
   * @param difficulty         難易度
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer, String difficulty) {
    player.sendTitle("採掘ゲームスタート!", "", 10, 40, 10);
    Bukkit.getScheduler().runTaskLater(main, () -> {
      player.sendTitle("お疲れさまでした!",
          nowExecutingPlayer.getPlayerName() + "さんは合計" + nowExecutingPlayer.getScore() + "点でした。",
          0, 60, 0);
      nowExecutingPlayer.setScore(0);

      try (Connection con = DriverManager.getConnection(
          "jdbc:mysql://localhost:3306/spigot_server",
          "root",
          "rootrootroot");
          Statement statement = con.createStatement()) {

        statement.executeUpdate(
            "insert player_score(player_name, score, difficulty, registered_at)"
                + "values('" + nowExecutingPlayer.getPlayerName() + "', "
                + nowExecutingPlayer.getScore()
                + ", '" + difficulty + "', now());");

      } catch (SQLException e) {
        e.printStackTrace();
      }

      removeGeneratedBlocks();
    }, GAME_TIME);

    List<Material> blocksList = getMaterials(difficulty);

    //ランダムに並べたMaterialの出現場所
    generateRandomBlocks(player, player.getWorld(), blocksList);
    // ここでallowedBlocksリストを生成したブロックの種類のみにリセットする
    allowedBlocks.clear();
    allowedBlocks.addAll(blocksList);
  }

  /**
   * コマンド実行しているプレイヤーのスコア情報を取得している。
   *
   * @param player コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer = new ExecutingPlayer(player.getName());
    if (executingPlayerList.isEmpty()) {
      executingPlayer = addNewPlayer(player);
    } else {
      executingPlayer = executingPlayerList.stream().findFirst().map(ps
          -> ps.getPlayerName().equals(player.getName())
          ? ps
          : addNewPlayer(player)).orElse(executingPlayer);
    }
    executingPlayer.setGameTime(GAME_TIME);
    return executingPlayer;
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
    int xMin = playerLocation.getBlockX() - 4;
    int xMax = playerLocation.getBlockX() + 4;
    int yMin = playerLocation.getBlockY() - 3;
    int yMax = playerLocation.getBlockY() + 3;
    int zMin = playerLocation.getBlockZ() - 4;
    int zMax = playerLocation.getBlockZ() + 4;

    for (int x = xMin; x <= xMax; x++) {
      for (int y = yMin; y <= yMax; y++) {
        for (int z = zMin; z <= zMax; z++) {
          Location blockLocation = new Location(world, x, y, z);
          double distanceToPlayer = blockLocation.distance(playerLocation);
          boolean isAirBlock = !blockLocation.getBlock().getType().isSolid();

          if (distanceToPlayer > 3.0 && isAirBlock) {
            if (count >= 100) {
              break;
            }

            Material blockType = blocksList.get(count);
            if (allowedBlocks.contains(blockType)) {
              world.getBlockAt(x, y, z).setType(blockType);
              count++;
              generatedBlocks.add(blockLocation);
            }
          }
        }
      }
    }
  }

  /**
   * 生成されるBlockのList
   *
   * @param difficulty 難易度
   * @return Blockのscoreと生成される数
   */
  private static List<Material> getMaterials(String difficulty) {
    //難易度毎でListに持たせたMaterialの任意の数を指定。
    List<Material> blocksList = new ArrayList<>();
    switch (difficulty) {
      case EASY -> {
        blocksList.addAll(Collections.nCopies(20, Material.STONE));
        blocksList.addAll(Collections.nCopies(20, Material.DIAMOND_ORE));
        blocksList.addAll(Collections.nCopies(20, Material.LAPIS_ORE));
        blocksList.addAll(Collections.nCopies(20, Material.BLACKSTONE));
        blocksList.addAll(Collections.nCopies(20, Material.IRON_ORE));
        Collections.shuffle(blocksList);
      }
      case NORMAL -> {
        blocksList.addAll(Collections.nCopies(25, Material.STONE));
        blocksList.addAll(Collections.nCopies(10, Material.DIAMOND_ORE));
        blocksList.addAll(Collections.nCopies(15, Material.LAPIS_ORE));
        blocksList.addAll(Collections.nCopies(25, Material.BLACKSTONE));
        blocksList.addAll(Collections.nCopies(25, Material.IRON_ORE));
        Collections.shuffle(blocksList);
      }
      case HARD -> {
        blocksList.addAll(Collections.nCopies(30, Material.STONE));
        blocksList.addAll(Collections.nCopies(5, Material.DIAMOND_ORE));
        blocksList.addAll(Collections.nCopies(5, Material.LAPIS_ORE));
        blocksList.addAll(Collections.nCopies(30, Material.BLACKSTONE));
        blocksList.addAll(Collections.nCopies(30, Material.IRON_ORE));
        Collections.shuffle(blocksList);
      }
    }

    return blocksList;
  }

  /**
   * ブロックのスコア情報
   *
   * @param brokenBlockType 採掘するブロックの種類
   * @return 採掘したブロックの点数
   */
  private static int getBlockScore(Material brokenBlockType) {
    int point = switch (brokenBlockType) {
      case STONE -> 5;
      case IRON_ORE -> 20;
      case BLACKSTONE -> 15;
      case DIAMOND_ORE -> 100;
      case LAPIS_ORE -> 75;
      default -> 0;
    };
    return point;
  }


  /**
   * 3回連続で同じブロックを採掘するとscoreが3倍になる。
   *
   * @param player          コマンド実行したプレイヤー
   * @param executingPlayer 採掘したブロック毎のscore
   * @param brokenBlockType Listに登録されているBlock
   * @param point           同じブロックを３回連続で採掘すると４回目以降はscoreが３倍になる。
   */
  private static void updatePlayerScore(Player player, Material brokenBlockType,
      ExecutingPlayer executingPlayer,
      int point) {
    if (brokenBlockType == executingPlayer.getLastMinedBlock()) {
      executingPlayer.incrementConsecutiveBlocksMined();
      if (executingPlayer.getConsecutiveBlocksMined() >= 3) {
        executingPlayer.setScore(executingPlayer.getScore() + point * 3); // 3回目以降は2倍の点数
      } else {
        executingPlayer.setScore(executingPlayer.getScore() + point);
      }
    } else {
      executingPlayer.resetConsecutiveBlocksMined();
      executingPlayer.setScore(executingPlayer.getScore() + point);
    }
    executingPlayer.setLastMinedBlock(brokenBlockType);
    player.sendMessage("採掘しました！現在のスコアは" + executingPlayer.getScore() + "点です。");
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