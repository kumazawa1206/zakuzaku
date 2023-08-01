package plugin.zakuzaku.data;

import lombok.Getter;
import lombok.Setter;

/**
 * ゲームを実行する際のスコア情報を扱うオブジェクト。 プレイヤー名、合計点数、日時などの情報を持つ。
 */
@Getter
@Setter
public class PlayerScore {

  private String playerName;
  private int score;
}
