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

package me.moros.bending.model.ability.util;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.entity.Entity;

public enum FireTick implements FireTickMethod {
	OVERWRITE(Entity::setFireTicks),
	LARGER((e, a) -> {
		if (e.getFireTicks() < a) e.setFireTicks(a);
	}),
	ACCUMULATE((e, a) -> e.setFireTicks(FastMath.max(0, e.getFireTicks()) + a));

	private final FireTickMethod method;

	FireTick(FireTickMethod method) {
		this.method = method;
	}

	@Override
	public void apply(@NonNull Entity entity, int amount) {
		method.apply(entity, amount);
	}
}

