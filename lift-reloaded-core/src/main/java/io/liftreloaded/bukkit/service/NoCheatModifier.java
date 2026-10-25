package io.liftreloaded.bukkit.service;

import io.liftreloaded.bukkit.LiftPlugin;
import io.liftreloaded.bukkit.model.BukkitConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

@UtilityClass
public class NoCheatModifier {

	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private static final LiftPlugin plugin = LiftPlugin.INSTANCE;
	public static final FixedMetadataValue CAN_FLY_METADATA_TRUE = new FixedMetadataValue(plugin.getDelegate(), true);
	public static final FixedMetadataValue CAN_FLY_METADATA_FALSE = new FixedMetadataValue(plugin.getDelegate(), false);
	public static final String CAN_FLY_KEY = "lift_canFly";
	private static final String[] MOVING_CHECKS = {"MOVING_NOFALL", "MOVING_SURVIVALFLY"};

	public static void setFlightRules(Player player) {
		player.setMetadata(CAN_FLY_KEY, player.getAllowFlight() ? CAN_FLY_METADATA_TRUE : CAN_FLY_METADATA_FALSE);
		player.setAllowFlight(true);
		if (config.isUseNoCheatPlus()) {
			updateNoCheatPlusExemptions(player, "exemptPermanently");
		}
	}

	public static void resetFlightRules(Player player) {
		List<MetadataValue> metadata = player.getMetadata(CAN_FLY_KEY);
		if (metadata.isEmpty()) {
			plugin.logWarn("Metadata " + CAN_FLY_KEY + " not found on player " + player.getName());
			return;
		}
		if (metadata.size() > 1) {
			plugin.logWarn("Player " + player.getName() + " has too many metadata for " + CAN_FLY_KEY);
		}
		player.setAllowFlight(metadata.get(0).asBoolean());
		player.removeMetadata(CAN_FLY_KEY, plugin.getDelegate());
		if (config.isUseNoCheatPlus()) {
			updateNoCheatPlusExemptions(player, "unexempt");
		}
	}

	/**
	 * Keeps the optional NoCheatPlus integration independent of its unpublished Maven artifact.
	 * The soft dependency guarantees class-loader access when NoCheatPlus is installed.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void updateNoCheatPlusExemptions(Player player, String methodName) {
		try {
			Class<? extends Enum> checkTypeClass = (Class<? extends Enum>) Class.forName(
					"fr.neatmonster.nocheatplus.checks.CheckType").asSubclass(Enum.class);
			Class<?> exemptionManagerClass = Class.forName(
					"fr.neatmonster.nocheatplus.hooks.NCPExemptionManager");
			Method method = exemptionManagerClass.getMethod(methodName, Player.class, checkTypeClass);
			for (String check : MOVING_CHECKS) {
				method.invoke(null, player, Enum.valueOf(checkTypeClass, check));
			}
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
				 InvocationTargetException | IllegalArgumentException e) {
			plugin.logWarn("Could not update NoCheatPlus exemptions for " + player.getName() + ": " + e.getMessage());
		}
	}
}
