package plugin.zakuzaku.command;

import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class gameStartCommand implements CommandExecutor {

  private final int stackAmount = 27;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      World world = player.getWorld();

      //ListにMaterialを登録
      List<Material> blocksList = List.of(Material.STONE, Material.BLACKSTONE, Material.DEEPSLATE);
      int random = new SplittableRandom().nextInt(3);
      world.getBlockAt(getCreateBlockslocation(player, world)).setType(blocksList.get(random));
    }
    return false;
  }

  /**
   * ブロックの出現エリアを取得します。 出現エリアは自分の位置からZ軸に＋３の値が設定されます。 X軸、Y軸はプレイヤーと同じ位置を取得します。
   *
   * @param player コマンド実行者
   * @param world  コマンド実行者が所属するワールド
   * @return ブロックの出現場所
   */
  private Location getCreateBlockslocation(Player player, World world) {
    Location playerlocation = player.getLocation();
    double x = playerlocation.getX();
    double y = playerlocation.getY();
    double z = playerlocation.getZ() + 3;
    return new Location(world, x, y, z);
  }
}
