package plugin.zakuzaku.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

/**
 * ゲームを実行する際のプレイヤー情報を扱うオブジェクト。 プレイヤー名、合計点数、日時などの情報を持つ。
 */
@Getter
@Setter
public class ExecutingPlayer {

  private String playerName;
  private int score;
  private int gameTime;
  private int consecutiveBlocksMined; // 連続して採掘したブロックの数
  private Material lastMinedBlock; //最後に採掘したブロックの種類を保存する変数
  private Plugin main;
  private List<Location> generatedBlocks = new ArrayList<>();

  public ExecutingPlayer(String playerName) {
    this.playerName = playerName;
  }


  public void incrementScore() {
    this.score++;
  }

  // 連続して採掘したブロックの数を取得する
  public int getConsecutiveBlocksMined() {
    return consecutiveBlocksMined;
  }

  // 連続して採掘したブロックの数をインクリメントする
  public void incrementConsecutiveBlocksMined() {
    this.consecutiveBlocksMined++;
  }

  // 連続して採掘したブロックの数をリセットする
  public void resetConsecutiveBlocksMined() {
    this.consecutiveBlocksMined = 0;
  }

  // 最後に採掘したブロックの種類を設定する
  public void setLastMinedBlock(Material lastMinedBlock) {
    this.lastMinedBlock = lastMinedBlock;
  }

  // 最後に採掘したブロックの種類を取得する
  public Material getLastMinedBlock() {
    return lastMinedBlock;
  }

}
