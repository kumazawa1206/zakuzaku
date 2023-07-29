package plugin.zakuzaku;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.zakuzaku.command.GameStartCommand;

public final class Main extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    GameStartCommand gameStartCommand = new GameStartCommand();
    Bukkit.getPluginManager().registerEvents(gameStartCommand, this);
    getCommand("gameStart").setExecutor(gameStartCommand);
  }
}
