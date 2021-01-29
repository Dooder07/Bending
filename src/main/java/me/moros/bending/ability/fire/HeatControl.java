/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.fire;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

public class HeatControl extends AbilityInstance implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private long startTime;

	public HeatControl(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		AbilityDescription selectedAbility = user.getSelectedAbility().orElse(null);
		if (selectedAbility == null || !user.canBend(selectedAbility) || !getDescription().equals(selectedAbility)) {
			return UpdateResult.CONTINUE;
		}
		long time = System.currentTimeMillis();
		if (!user.isSneaking()) {
			startTime = time;
		} else {
			ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
			if (time > startTime + userConfig.cookInterval && cook()) {
				startTime = System.currentTimeMillis();
			}
		}
		return UpdateResult.CONTINUE;
	}

	private boolean cook() {
		if (user instanceof BendingPlayer) {
			PlayerInventory inventory = ((BendingPlayer) user).getEntity().getInventory();
			Material heldItem = inventory.getItemInMainHand().getType();
			if (MaterialUtil.COOKABLE.containsKey(heldItem)) {
				ItemStack cooked = new ItemStack(MaterialUtil.COOKABLE.get(heldItem));
				inventory.addItem(cooked).values().forEach(item -> user.getWorld().dropItem(user.getHeadBlock().getLocation(), item));
				int amount = inventory.getItemInMainHand().getAmount();
				if (amount == 1) {
					inventory.clear(inventory.getHeldItemSlot());
				} else {
					inventory.getItemInMainHand().setAmount(amount - 1);
				}
				return true;
			}
		}
		return false;
	}

	private void act() {
		if (!user.canBend(getDescription())) return;
		boolean acted = false;
		Location center = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range)).toLocation(user.getWorld());
		for (Block block : WorldMethods.getNearbyBlocks(center, userConfig.radius, b -> WaterMaterials.isIceBendable(b) || MaterialUtil.isFire(b))) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) continue;
			acted = true;
			if (WaterMaterials.isIceBendable(block)) {
				Optional<TempBlock> tb = TempBlock.MANAGER.get(block);
				if (tb.isPresent()) {
					tb.get().revert();
				} else {
					TempBlock.create(block, Material.WATER, true);
				}
			} else {
				block.setType(Material.AIR);
			}
		}
		if (acted) user.setCooldown(getDescription(), userConfig.cooldown);
	}

	public static void act(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("HeatControl")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, HeatControl.class).ifPresent(HeatControl::act);
		}
	}

	public static boolean canBurn(User user) {
		AbilityDescription current = user.getSelectedAbility().orElse(null);
		if (current == null || !current.getName().equals("HeatControl") || !user.canBend(current)) {
			return true;
		}
		user.getEntity().setFireTicks(0);
		return false;
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.RADIUS)
		public double radius;
		@Attribute(Attribute.CHARGE_TIME)
		public long cookInterval;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "heatcontrol");

			cooldown = abilityNode.node("cooldown").getLong(2000);
			range = abilityNode.node("range").getDouble(12.0);
			radius = abilityNode.node("radius").getDouble(5.0);
			cookInterval = abilityNode.node("cook-interval").getLong(2000);
		}
	}
}

