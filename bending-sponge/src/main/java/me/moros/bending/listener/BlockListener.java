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

package me.moros.bending.listener;

import me.moros.bending.ability.earth.passive.Locksmithing;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.LockableImpl;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.material.WaterMaterials;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.DropItemEvent;

public class BlockListener extends SpongeListener {
  public BlockListener(Game game) {
    super(game);
  }

  @Listener(order = Order.EARLY)
  public void onBlockIgnite(ChangeBlockEvent.All event) {
    if (disabledWorld(event.world())) {
      return;
    }
    var cause = event.cause().first(Entity.class).orElse(null);
    if (cause != null && ActionLimiter.isLimited(cause.uniqueId(), ActionType.INTERACT_BLOCK)) {
      event.invalidateAll();
      event.setCancelled(true);
      return;
    }
    for (var transaction : event.transactions()) {
      var loc = transaction.original().location().orElse(null);
      if (!transaction.isValid() || loc == null) {
        continue;
      }
      var op = transaction.operation();
      var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
      if (op == Operations.PLACE.get()) {
        onBlockPlace(block);
      } else if (op == Operations.BREAK.get()) {
        onBlockBreak(transaction, block, cause);
      } else {
        transaction.setValid(TempBlock.MANAGER.isTemp(block));
      }
    }
  }

  private void onBlockPlace(Block block) {
    TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
  }

  private void onBlockBreak(BlockTransaction transaction, Block block, @Nullable Entity cause) {
    if (cause instanceof ServerPlayer player && handleLockedContainer(block, player)) {
      transaction.setValid(false);
      return;
    }
    if (WaterMaterials.isPlantBendable(block)) {
      User user = cause == null ? null : Registries.BENDERS.get(cause.uniqueId());
      if (user != null) {
        AbilityDescription desc = user.selectedAbility();
        if (desc != null && desc.sourcePlant() && user.canBend(desc)) {
          transaction.setValid(false);
          return;
        }
      }
    }
    TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
  }

  private boolean handleLockedContainer(Block block, ServerPlayer player) {
    var w = PlatformAdapter.toSpongeWorld(block.world());
    var tile = w.blockEntity(block.blockX(), block.blockY(), block.blockZ()).orElse(null);
    if (tile != null && tile.supports(Keys.LOCK_TOKEN)) {
      if (!Locksmithing.canBreak(PlatformAdapter.fromSpongeEntity(player), new LockableImpl(tile))) {
        Component name = tile.get(Keys.CUSTOM_NAME).orElse(null);
        if (name == null) {
          name = Component.translatable(block.type().translationKey());
        }
        player.sendActionBar(Component.translatable("container.isLocked").args(name));
        Sound.BLOCK_CHEST_LOCKED.asEffect().play(block);
        return true;
      }
    }
    return false;
  }

  @Listener(order = Order.EARLY)
  public void onBlockDropItem(DropItemEvent.Destruct event) { // TODO needs testing
    var snapshot = event.context().get(EventContextKeys.BLOCK_TARGET).orElse(null);
    var loc = snapshot == null ? null : snapshot.location().orElse(null);
    if (loc == null || disabledWorld(loc.world())) {
      return;
    }
    var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
    if (TempBlock.MANAGER.isTemp(block)) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.FIRST)
  public void onBlockPhysics(ChangeBlockEvent.All event, @First FallingBlock fallingBlock) {
    if (disabledWorld(event.world())) {
      return;
    }
    var type = PlatformAdapter.fromSpongeBlock(fallingBlock.blockState().get().type());
    if (type.hasGravity()) {
      for (var transaction : event.transactions()) {
        var loc = transaction.original().location().orElse(null);
        if (!transaction.isValid() || loc == null) {
          continue;
        }
        var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
        if (TempBlock.shouldIgnorePhysics(block)) {
          transaction.setValid(false);
          event.setCancelled(true);
          return;
        }
      }
    }
  }
}
