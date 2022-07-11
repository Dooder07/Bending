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

package me.moros.bending.ability.air;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.ability.water.FrostBreath;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirShield extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d center;

  private long currentPoint = 0;
  private long startTime;

  public AirShield(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
    startTime = System.currentTimeMillis();
    center = EntityUtil.entityCenter(user.entity());
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBuild(user.headBlock())) {
      return UpdateResult.REMOVE;
    }
    currentPoint++;
    center = EntityUtil.entityCenter(user.entity());
    double spacing = userConfig.radius / 4;
    for (int i = 1; i < 8; i++) {
      double y = (i * spacing) - userConfig.radius;
      double factor = 1 - (y * y) / (userConfig.radius * userConfig.radius);
      if (factor <= 0.2) {
        continue;
      }
      double x = userConfig.radius * factor * Math.cos(i * currentPoint);
      double z = userConfig.radius * factor * Math.sin(i * currentPoint);
      Vector3d loc = center.add(new Vector3d(x, y, z));
      ParticleUtil.air(loc).count(5).offset(0.2).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.AIR.play(user.world(), loc);
      }
    }

    for (Block b : WorldUtil.nearbyBlocks(user.world(), center, userConfig.radius, MaterialUtil::isFire)) {
      WorldUtil.tryCoolLava(user, b);
      WorldUtil.tryExtinguishFire(user, b);
    }
    CollisionUtil.handle(user, new Sphere(center, userConfig.radius), this::onEntityHit, false);
    return UpdateResult.CONTINUE;
  }

  private boolean onEntityHit(Entity entity) {
    Vector3d toEntity = new Vector3d(entity.getLocation()).subtract(center);
    Vector3d normal = toEntity.withY(0).normalize();
    double strength = ((userConfig.radius - toEntity.length()) / userConfig.radius) * userConfig.maxPush;
    strength = Math.max(0, Math.min(1, strength));
    EntityUtil.applyVelocity(this, entity, new Vector3d(entity.getVelocity()).add(normal.multiply(strength)));
    return false;
  }

  @Override
  public void onDestroy() {
    double factor = userConfig.duration == 0 ? 1 : System.currentTimeMillis() - startTime / (double) userConfig.duration;
    long cooldown = Math.min(1000, (long) (factor * userConfig.cooldown));
    user.addCooldown(description(), cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(new Sphere(center, userConfig.radius));
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FrostBreath) {
      for (Block block : WorldUtil.nearbyBlocks(user.world(), center, userConfig.radius, MaterialUtil::isTransparentOrWater)) {
        if (!user.canBuild(block)) {
          continue;
        }
        WorldUtil.tryBreakPlant(block);
        if (MaterialUtil.isAir(block) || MaterialUtil.isWater(block)) {
          long iceDuration = BendingProperties.instance().iceRevertTime(1500);
          TempBlock.ice().duration(iceDuration).build(block);
        }
      }
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4000;
    @Modifiable(Attribute.DURATION)
    private long duration = 10_000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 4;
    @Modifiable(Attribute.STRENGTH)
    private double maxPush = 2.6;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "airshield");
    }
  }
}
