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

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TravellingSource implements State {
  private final BlockData data;
  private StateChain chain;
  private final User user;
  private Block source;

  private boolean started = false;

  private final double minDistanceSq, maxDistanceSq;

  public TravellingSource(@NonNull User user, @NonNull BlockData data, double minDistance, double maxDistance) {
    this.user = user;
    this.data = data;
    this.minDistanceSq = minDistance * minDistance;
    this.maxDistanceSq = maxDistance * maxDistance;
  }

  @Override
  public void start(@NonNull StateChain chain) {
    if (started) {
      return;
    }
    this.chain = chain;
    source = chain.chainStore().stream().findFirst().orElse(null);
    started = source != null;
  }

  @Override
  public void complete() {
    if (!started) {
      return;
    }
    chain.chainStore().clear();
    chain.chainStore().add(source);
    chain.nextState();
  }

  @Override
  public @NonNull UpdateResult update() {
    if (!started) {
      return UpdateResult.REMOVE;
    }
    clean();
    Vector3d target = Vector3d.center(user.locBlock());
    Vector3d location = Vector3d.center(source);

    double distSq = target.distanceSq(location);
    if (maxDistanceSq > minDistanceSq && distSq > maxDistanceSq) {
      return UpdateResult.REMOVE;
    }
    if (target.distanceSq(location) < minDistanceSq) {
      complete();
      return UpdateResult.CONTINUE;
    }

    if (isValid(source.getRelative(BlockFace.UP)) && source.getY() < user.headBlock().getY()) {
      source = source.getRelative(BlockFace.UP);
    } else if (isValid(source.getRelative(BlockFace.DOWN)) && source.getY() > user.headBlock().getY()) {
      source = source.getRelative(BlockFace.DOWN);
    } else {
      Vector3d direction = target.subtract(location).normalize();
      Block nextBlock = location.add(direction).toBlock(user.world());
      if (source.equals(nextBlock)) {
        source = findPath(nextBlock);
      } else {
        source = nextBlock;
      }
    }
    if (source == null || !isValid(source) || !user.canBuild(source)) {
      return UpdateResult.REMOVE;
    }
    TempBlock.create(source, data, 200);
    return UpdateResult.CONTINUE;
  }

  private Block findPath(Block check) {
    Location dest = user.headBlock().getLocation();
    Block result = null;
    double minDistance = Double.MAX_VALUE;
    for (BlockFace face : BlockMethods.SIDES) {
      Block block = check.getRelative(face);
      if (!isValid(block)) {
        continue;
      }
      double d = block.getLocation().distanceSquared(dest);
      if (d < minDistance) {
        minDistance = d;
        result = block;
      }
    }
    return result;
  }

  private boolean isValid(Block block) {
    if (!TempBlock.isBendable(block)) {
      return false;
    }
    if (data.getMaterial() == Material.WATER) {
      return MaterialUtil.isTransparentOrWater(block);
    }
    return MaterialUtil.isTransparent(block);
  }

  private void clean() {
    if (source.getType() == data.getMaterial()) {
      TempBlock.createAir(source);
    }
  }
}
