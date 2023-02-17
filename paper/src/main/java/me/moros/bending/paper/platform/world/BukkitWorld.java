/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.paper.platform.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.raytrace.CompositeRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.paper.platform.BukkitDataHolder;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.bending.paper.platform.block.LockableImpl;
import me.moros.bending.paper.platform.particle.ParticleMapper;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.TileState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BukkitWorld(org.bukkit.World handle) implements World {
  @Override
  public BlockType getBlockType(int x, int y, int z) {
    var key = PlatformAdapter.fromNsk(handle().getType(x, y, z).getKey());
    return BlockType.registry().getIfExists(key).orElse(BlockType.VOID_AIR);
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return PlatformAdapter.fromBukkitData(handle().getBlockData(x, y, z));
  }

  @Override
  public AABB blockBounds(int x, int y, int z) {
    var b = handle().getBlockAt(x, y, z);
    BoundingBox box = b.getBoundingBox();
    if (box.getVolume() == 0 || !b.isCollidable()) {
      return AABB.dummy();
    }
    Vector3d min = Vector3d.of(box.getMinX(), box.getMinY(), box.getMinZ());
    Vector3d max = Vector3d.of(box.getMaxX(), box.getMaxY(), box.getMaxZ());
    return new AABB(min, max);
  }

  @Override
  public DataHolder blockMetadata(int x, int y, int z) {
    return BukkitDataHolder.nonPersistent(handle().getBlockAt(x, y, z));
  }

  @Override
  public boolean isTileEntity(Position position) {
    var state = handle().getBlockAt(position.blockX(), position.blockY(), position.blockZ()).getState(false);
    return state instanceof TileState;
  }

  @Override
  public @Nullable Lockable containerLock(Position position) {
    var container = handle().getBlockAt(position.blockX(), position.blockY(), position.blockZ()).getState(false);
    if (container instanceof org.bukkit.block.Lockable lockable) {
      return new LockableImpl(lockable);
    }
    return null;
  }

  @Override
  public boolean setBlockState(int x, int y, int z, BlockState state) {
    handle().setBlockData(x, y, z, PlatformAdapter.toBukkitData(state));
    return true;
  }

  @Override
  public List<Entity> nearbyEntities(AABB box, Predicate<Entity> predicate, int limit) {
    var min = new Vector(box.min.x(), box.min.y(), box.min.z());
    var max = new Vector(box.max.x(), box.max.y(), box.max.z());
    BoundingBox bb = BoundingBox.of(min, max);
    List<Entity> entities = new ArrayList<>();
    for (var bukkitEntity : handle().getNearbyEntities(bb)) {
      Entity entity = PlatformAdapter.fromBukkitEntity(bukkitEntity);
      if (predicate.test(entity)) {
        entities.add(entity);
        if (limit > 0 && entities.size() >= limit) {
          return entities;
        }
      }
    }
    return entities;
  }

  @Override
  public String name() {
    return handle().getName();
  }

  @Override
  public int minHeight() {
    return handle().getMinHeight();
  }

  @Override
  public int maxHeight() {
    return handle().getMaxHeight();
  }

  @Override
  public <T> void spawnParticle(ParticleContext<T> context) {
    var p = ParticleMapper.mapParticle(context.particle());
    if (p != null) {
      var data = ParticleMapper.mapParticleData(context);
      handle().spawnParticle(p, context.position().x(), context.position().y(), context.position().z(), context.count(),
        context.offset().x(), context.offset().y(), context.offset().z(), context.extra(), data, true);
    }
  }

  @Override
  public CompositeRayTrace rayTraceEntities(Context context, double range) {
    var start = new Vector(context.origin().x(), context.origin().y(), context.origin().z());
    var dir = new Vector(context.dir().x(), context.dir().y(), context.dir().z());
    AABB box = AABB.fromRay(context.origin(), context.dir(), context.raySize());
    BoundingBox bb = new BoundingBox(box.min.x(), box.min.y(), box.min.z(), box.max.x(), box.max.y(), box.max.z());
    Entity nearestHitEntity = null;
    RayTraceResult nearestHitResult = null;
    double nearestDistanceSq = Double.MAX_VALUE;
    for (var bukkitEntity : handle().getNearbyEntities(bb)) {
      Entity entity = PlatformAdapter.fromBukkitEntity(bukkitEntity);
      if (context.entityPredicate().test(entity)) {
        BoundingBox boundingBox = bukkitEntity.getBoundingBox().expand(context.raySize());
        RayTraceResult hitResult = boundingBox.rayTrace(start, dir, range);
        if (hitResult != null) {
          double distanceSq = start.distanceSquared(hitResult.getHitPosition());
          if (distanceSq < nearestDistanceSq) {
            nearestHitEntity = entity;
            nearestHitResult = hitResult;
            nearestDistanceSq = distanceSq;
          }
        }
      }
    }
    if (nearestHitEntity == null) {
      return RayTrace.miss(context.endPoint());
    }
    var pos = nearestHitResult.getHitPosition();
    return RayTrace.hit(Vector3d.of(pos.getX(), pos.getY(), pos.getZ()), nearestHitEntity);
  }

  @Override
  public boolean isDay() {
    return handle().getEnvironment() == Environment.NORMAL && handle().isDayTime();
  }

  @Override
  public boolean isNight() {
    return handle().getEnvironment() == Environment.NORMAL && !handle().isDayTime();
  }

  @Override
  public boolean breakNaturally(int x, int y, int z) {
    return handle.getBlockAt(x, y, z).breakNaturally();
  }

  @Override
  public Entity dropItem(Position pos, ItemSnapshot item, boolean canPickup) {
    var loc = new Location(handle(), pos.x(), pos.y(), pos.z());
    var droppedItem = handle().dropItem(loc, PlatformAdapter.toBukkitItem(item));
    droppedItem.setCanMobPickup(canPickup);
    droppedItem.setCanPlayerPickup(canPickup);
    return PlatformAdapter.fromBukkitEntity(droppedItem);
  }

  @Override
  public Entity createFallingBlock(Position pos, BlockState state, boolean gravity) {
    var loc = new Location(handle(), pos.x(), pos.y(), pos.z());
    var data = PlatformAdapter.toBukkitData(state);
    var bukkitEntity = handle().spawnFallingBlock(loc, data);
    bukkitEntity.setGravity(gravity);
    bukkitEntity.setDropItem(false);
    return PlatformAdapter.fromBukkitEntity(bukkitEntity);
  }

  @Override
  public Entity createArmorStand(Position pos, Item type, boolean gravity) {
    var loc = new Location(handle(), pos.x(), pos.y(), pos.z());
    var item = PlatformAdapter.toBukkitItem(type);
    var bukkitEntity = handle().spawn(loc, ArmorStand.class, as -> {
      as.setInvulnerable(true);
      as.setVisible(false);
      as.setGravity(gravity);
      as.getEquipment().setHelmet(item);
    });
    return PlatformAdapter.fromBukkitEntity(bukkitEntity);
  }

  @Override
  public int lightLevel(int x, int y, int z) {
    return handle().getBlockAt(x, y, z).getLightLevel();
  }

  @Override
  public Dimension dimension() {
    return switch (handle().getEnvironment()) {
      case NORMAL -> Dimension.OVERWORLD;
      case NETHER -> Dimension.NETHER;
      case THE_END -> Dimension.END;
      case CUSTOM -> Dimension.CUSTOM;
    };
  }

  @Override
  public CompletableFuture<?> loadChunkAsync(int x, int z) {
    return handle().getChunkAtAsync(x, z);
  }

  @Override
  public int viewDistance() {
    return handle().getViewDistance();
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return handle().audiences();
  }

  @Override
  public @NonNull Key key() {
    return PlatformAdapter.fromNsk(handle().getKey());
  }
}
