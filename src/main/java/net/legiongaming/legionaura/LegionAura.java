package net.legiongaming.legionaura;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.util.*;

public final class LegionAura extends JavaPlugin {
    private final Set<UUID> enabled = new HashSet<>();
    private int particleTick = 0;

    @Override public void onEnable() {
        saveDefaultConfig();
        long period = Math.max(1L, getConfig().getLong("check-period-ticks", 2L));
        getServer().getScheduler().runTaskTimer(this, this::tickAuras, 1L, period);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (args.length != 1) { msg(p,"messages.usage"); return true; }
        if (args[0].equalsIgnoreCase("on")) { enabled.add(p.getUniqueId()); msg(p,"messages.enabled"); return true; }
        if (args[0].equalsIgnoreCase("off")) { enabled.remove(p.getUniqueId()); msg(p,"messages.disabled"); return true; }
        msg(p,"messages.usage"); return true;
    }

    private void tickAuras() {
        double radius=getConfig().getDouble("radius",20), strength=getConfig().getDouble("push-strength",1.15),
               vertical=getConfig().getDouble("vertical-push",.12);
        particleTick++;

        for (UUID id : Set.copyOf(enabled)) {
            Player p=getServer().getPlayer(id);
            if (p==null || !p.isOnline() || p.isDead()) continue;
            Location center=p.getLocation();

            for (Entity e:p.getWorld().getNearbyEntities(center,radius,radius,radius)) {
                if (!(e instanceof Enemy mob) || mob.isDead()) continue;
                Vector away=mob.getLocation().toVector().subtract(center.toVector());
                double distance=away.length();
                if (distance<=.001 || distance>radius) continue;
                away.setY(0);
                if (away.lengthSquared()<.0001) away=p.getLocation().getDirection().multiply(-1).setY(0);
                away.normalize();
                double force=.28+(strength*(1-Math.min(distance/radius,1)));
                Vector velocity=away.multiply(force).setY(vertical);
                mob.setVelocity(velocity);
            }

            // Sparkling glow around the player's body while aura is active.
            if (getConfig().getBoolean("particles.enabled",true) && particleTick % 2 == 0) {
                Particle particle;
                try { particle=Particle.valueOf(getConfig().getString("particles.type","END_ROD").toUpperCase(Locale.ROOT)); }
                catch (Exception ex) { particle=Particle.END_ROD; }
                int count=Math.max(1,getConfig().getInt("particles.count",3));
                double spread=getConfig().getDouble("particles.spread",.32);
                double yspread=getConfig().getDouble("particles.vertical-spread",.75);
                Location glow=center.clone().add(0,1.0,0);
                p.getWorld().spawnParticle(particle,glow,count,spread,yspread,spread,0.01);
            }
        }
    }

    private void msg(Player p,String path) {
        p.sendMessage(ChatColor.translateAlternateColorCodes('&',getConfig().getString(path,"")));
    }
}
