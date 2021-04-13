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

package me.moros.bending.ability.air;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractBurst;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.methods.UserMethods;

public class AirBurst extends AbstractBurst implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private boolean released;
	private long startTime;

	public AirBurst(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (method == ActivationMethod.ATTACK) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, AirBurst.class)
				.ifPresent(b -> b.release(true));
			return false;
		}

		this.user = user;
		recalculateConfig();

		released = false;
		if (method == ActivationMethod.FALL) {
			if (user.getEntity().getFallDistance() < userConfig.fallThreshold || user.isSneaking()) {
				return false;
			}
			release(false);
		}
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (!released) {
			boolean charged = isCharged();
			if (charged) {
				ParticleUtil.createAir(UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release(false);
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		}
		return updateBurst();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	private void release(boolean cone) {
		if (released || !isCharged()) return;
		released = true;
		if (cone) {
			createCone(user, () -> new AirBlast(getDescription()), userConfig.coneRange);
		} else {
			createSphere(user, () -> new AirBlast(getDescription()), userConfig.sphereRange);
		}
		setRenderInterval(100);
		setRenderParticleCount(1);
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.CHARGE_TIME)
		public int chargeTime;
		@Attribute(Attribute.RANGE)
		public double sphereRange;
		@Attribute(Attribute.RANGE)
		public double coneRange;
		public double fallThreshold;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airburst");

			cooldown = abilityNode.node("cooldown").getLong(6000);
			chargeTime = abilityNode.node("charge-time").getInt(3500);
			coneRange = abilityNode.node("cone-range").getDouble(16.0);
			sphereRange = abilityNode.node("sphere-range").getDouble(12.0);
			fallThreshold = abilityNode.node("fall-threshold").getDouble(14.0);
		}
	}
}
