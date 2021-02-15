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

package me.moros.bending.ability.common;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class SelectedSource implements State {
	private StateChain chain;
	private final User user;
	private Vector3 origin;
	private Block block;
	private Material material;

	private final boolean particles;
	private final double distanceSq;

	private boolean started = false;

	public SelectedSource(@NonNull User user, @NonNull Block block, double maxDistance, @Nullable BlockData data) {
		this.user = user;
		this.distanceSq = maxDistance * maxDistance;
		particles = data == null;
		reselect(block, data);
	}

	public SelectedSource(@NonNull User user, @NonNull Block block, double maxDistance) {
		this(user, block, maxDistance, null);
	}

	public boolean reselect(@NonNull Block block) {
		return reselect(block, null);
	}

	public boolean reselect(@NonNull Block block, @Nullable BlockData data) {
		if (block.equals(this.block)) return false;
		Vector3 newOrigin = new Vector3(block).add(Vector3.HALF);
		if (user.getEyeLocation().distanceSq(newOrigin) > distanceSq) return false;
		onDestroy();
		this.block = block;
		this.origin = newOrigin;
		this.material = data == null ? block.getType() : data.getMaterial();
		if (data != null) TempBlock.create(block, data, false);
		return true;
	}

	@Override
	public void start(@NonNull StateChain chain) {
		if (started) return;
		this.chain = chain;
		started = true;
	}

	@Override
	public void complete() {
		if (!started) return;
		onDestroy();
		chain.getChainStore().clear();
		chain.getChainStore().add(block);
		chain.nextState();
	}

	@Override
	public @NonNull UpdateResult update() {
		if (!started) return UpdateResult.REMOVE;
		if (block.getType() != material || user.getEyeLocation().distanceSq(origin) > distanceSq) {
			return UpdateResult.REMOVE;
		}
		Location loc = origin.toLocation(user.getWorld());
		if (particles) {
			ParticleUtil.create(Particle.SMOKE_NORMAL, loc.add(0, 0.5, 0)).spawn();
		}
		return UpdateResult.CONTINUE;
	}

	public @NonNull Block getSelectedSource() {
		return block;
	}

	public void onDestroy() {
		if (!particles) {
			TempBlock.MANAGER.get(block).filter(tb -> tb.getBlock().getType() == material).ifPresent(TempBlock::revert);
		}
	}
}
