package fr.badblock.bukkit.games.survivalgames.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import fr.badblock.bukkit.games.survivalgames.PluginSurvival;
import fr.badblock.bukkit.games.survivalgames.players.SurvivalData;
import fr.badblock.gameapi.BadListener;
import fr.badblock.gameapi.GameAPI;
import fr.badblock.gameapi.disguise.Disguise;
import fr.badblock.gameapi.players.BadblockPlayer;
import fr.badblock.gameapi.utils.itemstack.ItemAction;
import fr.badblock.gameapi.utils.itemstack.ItemEvent;
import fr.badblock.gameapi.utils.itemstack.ItemStackExtra.ItemPlaces;
import fr.badblock.gameapi.utils.itemstack.ItemStackUtils;

public class ZombieListener extends BadListener {
	@EventHandler
	public void onDamage(EntityDamageEvent e){
		if(inGame()){
			return;
		}
		
		if(e.getCause() != DamageCause.ENTITY_ATTACK)
			e.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDamage(EntityDamageByEntityEvent e){
		if(inGame() || e.getDamager().getType() != EntityType.PLAYER || e.getEntityType() != EntityType.PLAYER)
			return;
		
		if(!isIn(e.getDamager().getLocation())){
			e.setCancelled(true);
			return;
		}
		
		BadblockPlayer damaged = (BadblockPlayer) e.getEntity();
		BadblockPlayer damager = (BadblockPlayer) e.getDamager();
		
		boolean damagerZombie = damager.inGameData(SurvivalData.class).zombie;
		boolean damagedZombie = damaged.inGameData(SurvivalData.class).zombie;
		
		if(damagerZombie == damagedZombie){
			e.setCancelled(true);
			return;
		}

		e.setDamage(0.0d);
		
		if(damagerZombie){
			if(damaged.getMaxHealth() <= 2.0d){
				GameAPI.getAPI().getOnlinePlayers().stream()
												   .filter(player -> isIn(player.getLocation()))
												   .forEach(player -> player.sendTranslatedMessage("survival.zombie.broadcast-kill", damager.getName(), damaged.getName()));;
												   
				zombify(damaged);
			} else {
				double maxHealth = damaged.getMaxHealth() - 2.0d;
				
				damaged.setMaxHealth(maxHealth);
				damaged.setHealth(maxHealth);
			}
		} else {
			Vector vector = new Vector(
					damaged.getLocation().getX() - damager.getLocation().getX(),
					0,
					damaged.getLocation().getZ() - damager.getLocation().getZ()
			);
			
			double distance = damager.getLocation().distance(damaged.getLocation());
			
			vector.divide(new Vector(distance, distance, distance));
			vector.multiply(2.0d);

			vector.setY(0.8);

			damaged.setVelocity(vector);
		}
	}
	
	@EventHandler
	public void onDisconnect(PlayerQuitEvent e){
		boolean must = GameAPI.getAPI().getOnlinePlayers().stream().filter(player -> player.inGameData(SurvivalData.class).zombie && player != e.getPlayer()).count() == 0;
		
		if(must){
			Optional<BadblockPlayer> opt = GameAPI.getAPI().getOnlinePlayers().stream().filter(player -> isIn(player.getLocation()) && player != e.getPlayer()).findAny();
		
			if(opt.isPresent()){
				zombify(opt.get());
			}
		}
	}
	
	
	@EventHandler
	public void onMove(PlayerMoveEvent e){
		if(!isIn(e.getFrom()) && isIn(e.getTo())){
			
			BadblockPlayer p = (BadblockPlayer) e.getPlayer();
			
			if(p.inGameData(SurvivalData.class).zombie)
				return;
			
			if(GameAPI.getAPI().getOnlinePlayers().stream().filter(player -> player.inGameData(SurvivalData.class).zombie).count() > 0){
				asPlayer(p);
				p.sendTranslatedMessage("survival.zombie.enter", p.getName());
			} else {
				p.sendTranslatedMessage("survival.zombie.enter-as-zombie", p.getName());
				zombify(p);
			}
			
		}
	}
	
	protected void asPlayer(BadblockPlayer p){
		p.setMaxHealth(6.0d);
		p.setHealth(6.0d);
	}
	
	protected void zombify(BadblockPlayer p){
		p.setMaxHealth(20.0d);
		p.setHealth(20.0d);
		p.disguise(new Disguise(EntityType.ZOMBIE, null, true, false));
		p.inGameData(SurvivalData.class).zombie = true;
		
		for(int i=0;i<p.getInventory().getSize();i++){
			if(ItemStackUtils.isValid(p.getInventory().getItem(i)))
				continue;
			
			ItemStack is = GameAPI.getAPI().createItemStackExtra(new ItemStack(Material.ROTTEN_FLESH)).listenAs(new ItemEvent(){
				@Override
				public boolean call(ItemAction action, BadblockPlayer player) {
					return true;
				}
			}, ItemPlaces.HOTBAR_UNCLICKABLE).getHandler();
		
			p.getInventory().setItem(i, is);
		}
	}
	
	protected boolean isIn(Location loc){
		return PluginSurvival.getInstance().getConfiguration().zombieGame.getHandle().isInSelection(loc);
	}
}
