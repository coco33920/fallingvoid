package fr.gravenilvec.fallingvoid;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.gravenilvec.fallingvoid.tasks.FallingAutoStart;

public class FallingListeners implements Listener {

    private FallingVoid main;
    private static final String PREFIX = "prefix";

    public FallingListeners(FallingVoid fallingVoid) {
        this.main = fallingVoid;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(main.getSpawn());
        player.setHealth(20D);
        player.getInventory().clear();
        player.getEnderChest().clear();

        if (!main.isState(FallingState.WAITING) && !main.isState(FallingState.STARTING)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(main.get(PREFIX) + main.get("alreadyStart"));
            event.setJoinMessage(null);
            return;
        }

        if (!main.playersList.contains(player)) {

            main.playersList.add(player);
            event.setJoinMessage(main.get(PREFIX) + main.get("join").replace("<online>", "" + main.playersList.size()).replace("<maxonline>", Integer.toString(Bukkit.getMaxPlayers())).replace("<player>", player.getName()));

            if (main.isState(FallingState.WAITING) && main.playersList.size() == main.getConfig().getInt("autostart.minplayers")) {
                main.setState(FallingState.STARTING);
                FallingAutoStart start = new FallingAutoStart(main);
                start.runTaskTimer(main, 0, 20);
            }

        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        main.eliminate(player);
        event.setQuitMessage(main.get(PREFIX) + main.get("left").replace("<player>", player.getName()));
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location l = player.getLocation();

        if (l.getY() < main.getConfig().getInt("blocks.autoKillHeight") && !main.isState(FallingState.WAITING) && !main.isState(FallingState.STARTING)) {
            main.eliminate(player);
            return;
        }

        if (l.getBlockY() > 0 && main.isState(FallingState.WAITING) && main.isState(FallingState.STARTING)) {
            player.teleport(main.getSpawn());
        }

    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {

        if (event.getBlock().getLocation().getY() > main.getConfig().getInt("blocks.maxHeight")) {
            event.setCancelled(true);
            return;
        }

    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        if (!main.isState(FallingState.PREGAME) && !main.isState(FallingState.GAME) && !main.isState(FallingState.GAMEBORDER)) {
            event.setCancelled(true);
            return;
        }

    }


    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();

            if (!main.isState(FallingState.GAME) && !main.isState(FallingState.GAMEBORDER)) {
                event.setCancelled(true);
                return;
            }

            if (event.getCause() == DamageCause.FALL && victim.getHealth() <= event.getDamage()) {
                event.setDamage(0);
                main.eliminate(victim);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (victim.getHealth() <= event.getDamage()) {
                event.setDamage(0);
                main.eliminate(victim);
            }
        }
    }

}
