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

package me.moros.bending.ability.fire;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.AbilityInitializer;
import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireBlast extends AbilityInstance implements Explosive {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private Collider ignoreCollider;

  private boolean charging;
  private boolean exploded = false;
  private double factor = 1.0;
  private long startTime;

  public FireBlast(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.mainHandSide().toBlock(user.world()).isLiquid()) {
      return false;
    }

    removalPolicy = Policies.builder().add(Policies.UNDER_WATER).add(Policies.UNDER_LAVA).build();

    for (FireBlast blast : user.game().abilityManager(user.world()).userInstances(user, FireBlast.class).toList()) {
      if (blast.charging) {
        blast.launch();
        return false;
      }
    }
    if (method == Activation.ATTACK) {
      launch();
    }
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (charging) {
      if (!description().equals(user.selectedAbility())) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleUtil.fire(user, user.mainHandSide()).spawn(user.world());
      } else if (!user.sneaking()) {
        launch();
      }
      return UpdateResult.CONTINUE;
    }
    return stream.update();
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    long cooldown = userConfig.cooldown;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
      cooldown = userConfig.chargedCooldown;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), cooldown);
    Vector3d origin = user.mainHandSide();
    Vector3d lookingDir = user.direction().multiply(userConfig.range * factor);
    stream = new FireStream(new Ray(origin, lookingDir));
  }

  @Override
  public Collection<Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    boolean fullyCharged = factor == userConfig.chargeFactor;
    if (fullyCharged && collision.removeSelf()) {
      String name = collidedAbility.description().name();
      if (AbilityInitializer.layer2.contains(name)) {
        collision.removeOther(true);
      } else {
        collision.removeSelf(false);
      }
    }
    if (fullyCharged && collidedAbility instanceof FireShield fireShield) {
      collision.removeOther(true);
      if (fireShield.isSphere()) {
        ignoreCollider = collision.colliderOther();
        explode();
      }
    } else if (collidedAbility instanceof FireBlast other) {
      double collidedFactor = other.factor;
      if (fullyCharged && collidedFactor == other.userConfig.chargeFactor) {
        Vector3d first = collision.colliderSelf().position();
        Vector3d second = collision.colliderOther().position();
        Vector3d center = first.add(second).multiply(0.5);
        double radius = userConfig.explosionRadius + other.userConfig.explosionRadius;
        double dmg = userConfig.damage + other.userConfig.damage;
        createExplosion(center, radius, dmg * (factor + other.factor - 1));
        other.exploded = true;
      } else if (factor > collidedFactor + 0.1) {
        collision.removeSelf(false);
      }
    } else if (fullyCharged && collidedAbility instanceof Explosive) {
      explode();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void explode() {
    createExplosion(stream.location(), userConfig.explosionRadius, userConfig.damage * factor);
  }

  private void createExplosion(Vector3d center, double size, double damage) {
    if (exploded || factor < userConfig.chargeFactor) {
      return;
    }
    exploded = true;
    BendingExplosion.builder()
      .size(size)
      .damage(damage)
      .fireTicks(userConfig.fireTicks)
      .ignoreInsideCollider(ignoreCollider)
      .sound(5, 1)
      .buildAndExplode(this, center);
  }

  private class FireStream extends ParticleStream {
    private final double offset;
    private final double particleSpeed;
    private final int amount;
    private final boolean explosive;

    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed * factor, 0.8 + 0.5 * (factor - 1));
      canCollide = Block::isLiquid;
      offset = 0.25 + (factor - 1);
      particleSpeed = 0.02 * factor;
      amount = FastMath.ceil(6 * Math.pow(factor, 4));
      explosive = factor == userConfig.chargeFactor;
      singleCollision = explosive;
    }

    @Override
    public void render() {
      ParticleUtil.fire(user, location).count(amount).offset(offset).extra(particleSpeed).spawn(user.world());
      TempLight.builder(++ticks).build(location.toBlock(user.world()));
    }

    @Override
    public void postRender() {
      if (explosive || ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (explosive) {
        explode();
        return true;
      }
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
      EntityUtil.applyVelocity(FireBlast.this, entity, ray.direction.normalize().multiply(0.5));
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      Vector3d reverse = ray.direction.negate();
      WorldUtil.tryLightBlock(block);
      Vector3d standing = user.location().add(0, 0.5, 0);
      for (Block b : WorldUtil.nearbyBlocks(user.world(), location, userConfig.igniteRadius * factor)) {
        if (standing.distanceSq(Vector3d.center(b)) < 4 || !user.canBuild(b)) {
          continue;
        }
        if (user.rayTrace(Vector3d.center(b), reverse).range(userConfig.igniteRadius + 2).blocks(user.world()).hit()) {
          continue;
        }
        if (MaterialUtil.isIgnitable(b)) {
          TempBlock.fire().duration(BendingProperties.instance().fireRevertTime(1000))
            .ability(FireBlast.this).build(b);
        }
      }
      FragileStructure.tryDamageStructure(block, FastMath.round(4 * factor), new Ray(location, ray.direction));
      explode();
      return true;
    }

    private Vector3d location() {
      return location;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 25;
    @Modifiable(Attribute.RANGE)
    private double range = 18;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    @Modifiable(Attribute.RADIUS)
    private double igniteRadius = 1.5;
    @Modifiable(Attribute.RADIUS)
    private double explosionRadius = 2.5;
    @Comment("How much the damage, radius, range and speed are multiplied by at full charge")
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 1.5;
    @Comment("How many milliseconds it takes to fully charge")
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 1500;
    @Modifiable(Attribute.COOLDOWN)
    private long chargedCooldown = 500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "fireblast");
    }
  }
}
