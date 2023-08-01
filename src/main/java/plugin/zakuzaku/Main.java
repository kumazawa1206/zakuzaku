package plugin.zakuzaku;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.zakuzaku.command.GameStartCommand;

public final class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    GameStartCommand gameStartCommand = new GameStartCommand(this);
    Bukkit.getPluginManager().registerEvents(gameStartCommand, this);
    getCommand("gameStart").setExecutor(gameStartCommand);
  }
}
