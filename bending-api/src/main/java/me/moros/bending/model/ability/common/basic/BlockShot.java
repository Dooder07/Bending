/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.ability.common.basic;

import java.util.function.Predicate;

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import me.moros.math.VectorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BlockShot implements Updatable, SimpleAbility {
  private User user;

  private Vector3d location;
  private Block previousBlock;
  private Block tempBlock;
  private AABB collider;
  private final Vector3d firstDestination;

  protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparentOrWater(b);
  protected Vector3d target;
  protected Vector3d direction;
  protected Material material;

  private boolean settingUp;
  private int buffer;
  private final int speed;

  protected boolean allowUnderWater = false;
  protected final double range;

  /**
   * The maximum speed is 20 and represents movement of 1 block per tick.
   * We multiply speed steps by 100 to allow enough control over speed while ensuring accuracy.
   */
  protected BlockShot(User user, Block block, Material material, double range, int speed) {
    this.user = user;
    this.material = material;
    this.location = Vector3d.fromCenter(block);
    this.collider = AABB.EXPANDED_BLOCK_BOUNDS.at(location.floor());
    this.range = range;
    this.speed = Math.min(20, speed);
    buffer = speed;

    redirect();
    settingUp = true;
    int targetY = FastMath.floor(target.y());
    int currentY = block.getY();
    Vector3d dir = target.subtract(location).normalize().withY(0);
    boolean validAbove = block.getRelative(BlockFace.UP).isPassable();
    boolean validBelow = block.getRelative(BlockFace.DOWN).isPassable();
    Vector3d fixedY = location.withY(targetY + 0.5);
    if ((targetY > currentY && validAbove) || (targetY < currentY && validBelow)) {
      firstDestination = fixedY;
    } else if (location.add(dir).toBlock(user.world()).isPassable()) {
      firstDestination = location.add(dir).blockCenter();
    } else {
      if (validAbove) {
        firstDestination = location.add(0, 2, 0);
      } else if (validBelow) {
        firstDestination = location.add(0, -2, 0);
      } else {
        firstDestination = fixedY;
      }
    }
    direction = firstDestination.subtract(location).normalize();
  }

  @Override
  public UpdateResult update() {
    buffer += speed;
    if (buffer < 20) {
      return UpdateResult.CONTINUE;
    }
    buffer -= 20;

    clean();
    if (Math.abs(location.y() - firstDestination.y()) < 0.5) {
      settingUp = false;
    }
    previousBlock = location.toBlock(user.world());
    Vector3d dest = settingUp ? firstDestination : target;
    direction = dest.subtract(location.subtract(direction)).normalize();
    Vector3d originalVector = location;
    Block originBlock = originalVector.toBlock(user.world());
    if (location.add(direction).toBlock(user.world()).equals(previousBlock)) {
      direction = direction.multiply(2);
    }
    for (Vector3i v : VectorUtil.decomposeDiagonals(originalVector, direction)) {
      Block diagonal = originBlock.getRelative(v.blockX(), v.blockY(), v.blockZ());
      if (diagonalsPredicate.test(diagonal)) {
        onBlockHit(diagonal);
        return UpdateResult.REMOVE;
      }
    }
    if (originalVector.add(direction).distanceSq(user.eyeLocation()) > range * range) {
      return UpdateResult.REMOVE;
    }
    location = location.add(direction);
    Block current = location.toBlock(user.world());
    if (!user.canBuild(current)) {
      return UpdateResult.REMOVE;
    }
    collider = AABB.EXPANDED_BLOCK_BOUNDS.at(location.floor());
    if (CollisionUtil.handle(user, collider, this)) {
      return UpdateResult.REMOVE;
    }
    if (MaterialUtil.isTransparent(current) || (MaterialUtil.isWater(current) && allowUnderWater)) {
      WorldUtil.tryBreakPlant(current);
      if (material == Material.WATER && MaterialUtil.isWater(current)) {
        ParticleUtil.bubble(current).spawn(user.world());
        tempBlock = null;
      } else {
        tempBlock = current;
        TempBlock.builder(material.createBlockData()).build(current);
      }
      postRender();
    } else {
      onBlockHit(current);
      return UpdateResult.REMOVE;
    }

    if (location.distanceSq(target) < 0.8) {
      // Project the target block
      Block projected = location.add(direction).toBlock(user.world());
      if (!MaterialUtil.isTransparent(projected)) {
        onBlockHit(projected);
      }
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  public void redirect() {
    target = user.rayTrace(range).ignore(location).entities(user.world()).entityEyeLevelOrPosition().blockCenter();
    settingUp = false;
  }

  public @Nullable Block previousBlock() {
    return previousBlock;
  }

  public Vector3d center() {
    return location.blockCenter();
  }

  @Override
  public void render() {
  }

  @Override
  public AABB collider() {
    return collider;
  }

  public boolean isValid(Block block) {
    if (material == Material.WATER) {
      return MaterialUtil.isWater(block);
    }
    return material == block.getType();
  }

  public void clean() {
    if (tempBlock != null) {
      clean(tempBlock);
    }
  }

  public void clean(Block block) {
    if (isValid(block)) {
      TempBlock.air().build(block);
    }
  }

  public void user(User user) {
    this.user = user;
  }
}
