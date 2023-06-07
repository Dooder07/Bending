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

package me.moros.bending.fabric.mixin.entity;

import me.moros.bending.fabric.event.ServerEntityEvents;
import me.moros.bending.fabric.event.ServerItemEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin extends EntityMixin {
  @ModifyVariable(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), ordinal = 0, argsOnly = true)
  private float bending$onHurt(float originalValue, DamageSource source, float amount) {
    return (float) ServerEntityEvents.DAMAGE.invoker().onDamage(get(), source, originalValue);
  }

  @Shadow
  protected Player lastHurtByPlayer;

  @Shadow
  public abstract ResourceLocation getLootTable();

  @Shadow
  public abstract long getLootTableSeed();

  @Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
  private void bending$overrideDropFromLootTable(DamageSource damageSource, boolean bl, CallbackInfo ci) {
    ResourceLocation resourceLocation = this.getLootTable();
    LootTable lootTable = this.level().getServer().getLootData().getLootTable(resourceLocation);
    LootParams.Builder builder = new LootParams.Builder((ServerLevel) this.level()).withParameter(LootContextParams.THIS_ENTITY, get())
      .withParameter(LootContextParams.ORIGIN, this.position())
      .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
      .withOptionalParameter(LootContextParams.KILLER_ENTITY, damageSource.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, damageSource.getDirectEntity());
    if (bl && this.lastHurtByPlayer != null) {
      builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
    }
    LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
    var items = lootTable.getRandomItems(lootParams, this.getLootTableSeed());
    var result = ServerItemEvents.ENTITY_DROP_LOOT.invoker().onDropLoot(get(), damageSource, items);
    if (result.getResult() != InteractionResult.FAIL) {
      result.getObject().forEach(this::spawnAtLocation);
    }
    ci.cancel();
  }

  private LivingEntity get() {
    return (LivingEntity) (Object) this;
  }
}
