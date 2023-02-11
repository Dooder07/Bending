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

package me.moros.bending.fabric.platform;

import java.util.Objects;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.damage.DamageCause;
import me.moros.bending.api.platform.entity.DelegateEntity;
import me.moros.bending.api.platform.entity.DelegateLivingEntity;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.fabric.platform.block.FabricBlockState;
import me.moros.bending.fabric.platform.entity.FabricEntity;
import me.moros.bending.fabric.platform.entity.FabricLivingEntity;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import me.moros.bending.fabric.platform.item.FabricItem;
import me.moros.bending.fabric.platform.world.FabricWorld;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static MobEffect toFabricPotion(PotionEffect effect) {
    return Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.get(rsl(effect.key())));
  }

  public static MobEffectInstance toFabricPotion(Potion p) {
    return new MobEffectInstance(toFabricPotion(p.effect()), p.duration(), p.amplifier(), p.ambient(), p.particles(), p.icon());
  }

  public static Potion fromFabricPotion(MobEffectInstance p) {
    var effect = PotionEffect.registry().getOrThrow(BuiltInRegistries.MOB_EFFECT.getKey(p.getEffect()));
    return Potion.builder(effect).duration(p.getDuration()).amplifier(p.getAmplifier())
      .ambient(p.isAmbient()).particles(p.isVisible()).icon(p.showIcon()).build();
  }

  public static PotionEffectTag potionCategory(MobEffect type) {
    return switch (type.getCategory()) {
      case BENEFICIAL -> PotionEffectTag.BENEFICIAL;
      case NEUTRAL -> PotionEffectTag.NEUTRAL;
      case HARMFUL -> PotionEffectTag.HARMFUL;
    };
  }

  public static Item fromFabricItem(net.minecraft.world.item.Item material) {
    return Item.registry().getOrThrow(BuiltInRegistries.ITEM.getKey(material));
  }

  public static BlockType fromFabricBlock(net.minecraft.world.level.block.Block material) {
    return BlockType.registry().getOrThrow(BuiltInRegistries.BLOCK.getKey(material));
  }

  public static net.minecraft.world.item.Item toFabricItemType(Item item) {
    return BuiltInRegistries.ITEM.get(rsl(item.key()));
  }

  public static ItemStack toFabricItem(Item item) {
    return new ItemStack(toFabricItemType(item));
  }

  public static ItemStack toFabricItem(ItemSnapshot item) {
    return ((FabricItem) item).handle();
  }

  public static ItemSnapshot fromFabricItem(ItemStack itemStack) {
    return new FabricItem(itemStack);
  }

  public static ResourceLocation rsl(Key key) {
    return (ResourceLocation) key.key();
  }

  public static World fromFabricWorld(ServerLevel world) {
    return new FabricWorld(world);
  }

  public static ServerLevel toFabricWorld(World world) {
    return ((FabricWorld) world).handle();
  }

  public static Block fromFabricWorld(BlockSource block) {
    var pos = block.getPos();
    return fromFabricWorld(block.getLevel()).blockAt(pos.getX(), pos.getY(), pos.getZ());
  }

  public static me.moros.bending.api.platform.entity.Entity fromFabricEntity(Entity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromFabricEntity(player);
    } else if (entity instanceof LivingEntity living) {
      return PlatformAdapter.fromFabricEntity(living);
    } else {
      return new FabricEntity(entity);
    }
  }

  public static me.moros.bending.api.platform.entity.LivingEntity fromFabricEntity(LivingEntity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromFabricEntity(player);
    } else {
      return new FabricLivingEntity(entity);
    }
  }

  public static Player fromFabricEntity(ServerPlayer entity) {
    return new FabricPlayer(entity);
  }

  public static Entity toFabricEntity(me.moros.bending.api.platform.entity.Entity entity) {
    if (entity instanceof DelegateEntity delegate) {
      return toFabricEntity(delegate.entity());
    }
    return ((FabricEntity) entity).handle();
  }

  public static LivingEntity toFabricEntity(me.moros.bending.api.platform.entity.LivingEntity entity) {
    if (entity instanceof DelegateLivingEntity delegate) {
      return toFabricEntity(delegate.entity());
    }
    return ((FabricLivingEntity) entity).handle();
  }

  public static ServerPlayer toFabricEntity(Player player) {
    if (player instanceof DelegatePlayer delegate) {
      return toFabricEntity(delegate.entity());
    }
    return ((FabricPlayer) player).handle();
  }

  public static DamageCause fromFabricCause(DamageSource type) {
    if (type.isExplosion()) {
      return DamageCause.EXPLOSION;
    }
    if (type.isFire() && !(type instanceof AbilityDamageSource)) {
      return DamageCause.FIRE;
    } else if (type == DamageSource.FALL) {
      return DamageCause.FALL;
    } else if (type == DamageSource.FLY_INTO_WALL) {
      return DamageCause.KINETIC;
    } else if (type == DamageSource.IN_WALL) {
      return DamageCause.SUFFOCATION;
    } else {
      return DamageCause.CUSTOM;
    }
  }

  public static BlockState fromFabricData(net.minecraft.world.level.block.state.BlockState data) {
    return new FabricBlockState(data);
  }

  public static net.minecraft.world.level.block.state.BlockState toFabricData(BlockState state) {
    return ((FabricBlockState) state).handle();
  }
}
