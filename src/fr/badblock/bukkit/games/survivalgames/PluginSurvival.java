package fr.badblock.bukkit.games.survivalgames;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;

import fr.badblock.bukkit.games.survivalgames.commands.GameCommand;
import fr.badblock.bukkit.games.survivalgames.commands.SurvivalCommand;
import fr.badblock.bukkit.games.survivalgames.configuration.SurvivalConfiguration;
import fr.badblock.bukkit.games.survivalgames.configuration.SurvivalMapConfiguration;
import fr.badblock.bukkit.games.survivalgames.listeners.CraftListener;
import fr.badblock.bukkit.games.survivalgames.listeners.DamageListener;
import fr.badblock.bukkit.games.survivalgames.listeners.DeathListener;
import fr.badblock.bukkit.games.survivalgames.listeners.HostListener;
import fr.badblock.bukkit.games.survivalgames.listeners.JoinListener;
import fr.badblock.bukkit.games.survivalgames.listeners.MoveListener;
import fr.badblock.bukkit.games.survivalgames.listeners.SurvivalMapProtector;
import fr.badblock.bukkit.games.survivalgames.listeners.ZombieListener;
import fr.badblock.bukkit.games.survivalgames.players.SurvivalScoreboard;
import fr.badblock.bukkit.games.survivalgames.runnables.PreStartRunnable;
import fr.badblock.gameapi.BadblockPlugin;
import fr.badblock.gameapi.GameAPI;
import fr.badblock.gameapi.achievements.AchievementList;
import fr.badblock.gameapi.game.GameServer.WhileRunningConnectionTypes;
import fr.badblock.gameapi.game.rankeds.RankedManager;
import fr.badblock.gameapi.players.kits.PlayerKit;
import fr.badblock.gameapi.run.BadblockGame;
import fr.badblock.gameapi.run.BadblockGameData;
import fr.badblock.gameapi.run.RunType;
import fr.badblock.gameapi.utils.BukkitUtils;
import fr.badblock.gameapi.utils.GameRules;
import fr.badblock.gameapi.utils.general.JsonUtils;
import lombok.Getter;
import lombok.Setter;

public class PluginSurvival extends BadblockPlugin {
	@Getter private static PluginSurvival instance;
	
	public static 	     File   MAP;
	
	private static final String CONFIG 		   		   = "config.json";
	private static final String VOTES_CONFIG 		   = "votes.json";
	private static final String CHESTS_CONFIG 		   = "chests.json";
	private static final String KITS_CONFIG_INVENTORY  = "kitInventory.yml";
	private static final String MAPS_CONFIG_FOLDER     = "maps";

	@Getter@Setter
	private int 			         maxPlayers;
	@Getter
	private SurvivalConfiguration    configuration;
	@Getter@Setter
	private SurvivalMapConfiguration mapConfiguration;
	
	@Getter
	private Map<String, PlayerKit> kits;

	@Override
	public void onEnable(RunType runType){
		AchievementList list = SGAchievementList.instance;
		
		BadblockGame.SURVIVAL_GAMES.setGameData(new BadblockGameData() {
			@Override
			public AchievementList getAchievements() {
				return list;
			}
		});
		
		instance = this;
		
		if(runType == RunType.LOBBY)
			return;
		
		try {
			if(!getDataFolder().exists()) getDataFolder().mkdir();

			/**
			 * Chargement de la configuration du jeu
			 */

			// Modification des GameRules
			GameRules.doDaylightCycle.setGameRule(false);
			GameRules.spectatorsGenerateChunks.setGameRule(false);
			GameRules.doFireTick.setGameRule(false);

			// Lecture de la configuration du jeu

			BadblockGame.SURVIVAL_GAMES.use();

			File configFile    = new File(getDataFolder(), CONFIG);
			this.configuration = JsonUtils.load(configFile, SurvivalConfiguration.class);
			
			JsonUtils.save(configFile, configuration, true);
			
			getAPI().setDefaultKitContentManager(true);
			
			maxPlayers = configuration.maxPlayers;
			try {
				BukkitUtils.setMaxPlayers(configuration.maxPlayers);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
			kits	   = getAPI().loadKits(GameAPI.getInternalGameName());
			
			// Chargement des fonctionnalités de l'API non utilisées par défaut

			getAPI().getChestGenerator().setConfigurationFile(new File(getDataFolder(), CHESTS_CONFIG));
			getAPI().getChestGenerator().setRemoveOnOpen(true);
			
			getAPI().getBadblockScoreboard().doBelowNameHealth();
			getAPI().getBadblockScoreboard().doTabListHealth();
			getAPI().getBadblockScoreboard().doGroupsPrefix();;
			getAPI().getBadblockScoreboard().doOnDamageHologram();

			getAPI().formatChat(true, false);
			
			getAPI().getJoinItems().registerKitItem(0, kits, new File(getDataFolder(), KITS_CONFIG_INVENTORY));
			getAPI().getJoinItems().registerAchievementsItem(3, BadblockGame.SURVIVAL_GAMES);
			getAPI().getJoinItems().registerVoteItem(5);
			getAPI().getJoinItems().registerLeaveItem(8, configuration.fallbackServer);
			
			getAPI().setMapProtector(new SurvivalMapProtector());
			
			getAPI().getGameServer().whileRunningConnection(WhileRunningConnectionTypes.SPECTATOR);
			
			new MoveListener();
			new DeathListener();
			new JoinListener();
			new DamageListener();
			new CraftListener();
			new HostListener();
			new ZombieListener();
			
			File votesFile = new File(getDataFolder(), VOTES_CONFIG);

			if(!votesFile.exists())
				votesFile.createNewFile();

			getAPI().getBadblockScoreboard().beginVote(JsonUtils.loadArray(votesFile));
			
			new PreStartRunnable().runTaskTimer(GameAPI.getAPI(), 0, 30L);
			
			MAP = new File(getDataFolder(), MAPS_CONFIG_FOLDER);
			
			new SurvivalCommand(MAP);
			new GameCommand();
			
			Bukkit.getWorlds().forEach(world -> {
				world.setTime(2000L);
				world.getEntities().forEach(entity -> entity.remove());
			});
			
			// Ranked
			RankedManager.instance.initialize(RankedManager.instance.getCurrentRankedGameName(), 
					SurvivalScoreboard.KILLS, SurvivalScoreboard.DEATHS, SurvivalScoreboard.WINS, SurvivalScoreboard.LOOSES);
			
		} catch(Throwable e){
			e.printStackTrace();
		}
	}
	
	public void saveJsonConfig(){
		File configFile = new File(getDataFolder(), CONFIG);
		JsonUtils.save(configFile, configuration, true);
	}
}
