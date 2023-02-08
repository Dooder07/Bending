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

package me.moros.bending.fabric.listener;

import java.util.List;
import java.util.function.Supplier;

import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.common.ability.earth.passive.Locksmithing;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.fabric.event.ServerBlockEvents;
import me.moros.bending.fabric.event.ServerItemEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BlockListener(Supplier<Game> gameSupplier) implements FabricListener, Initializer {
  @Override
  public void init() {
    ServerPlayerEvents.PLACE_BLOCK.register(this::onBlockPlace);
    PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreak);
    PlayerBlockBreakEvents.AFTER.register(this::onAfterBlockBreak);
    ServerItemEvents.BLOCK_DROP_LOOT.register(this::onBlockDropLoot);
    ServerBlockEvents.CHANGE.register(this::onBlockChange);
    ServerBlockEvents.SPREAD.register(this::onBlockSpread);
    ServerBlockEvents.PISTON.register(this::onBlockPistonEvent);
  }

  private boolean onBlockPlace(ServerPlayer player, BlockPos pos, BlockState state) {
    if (!disabledWorld(player)) {
      if (ActionLimiter.isLimited(player.getUUID(), ActionType.INTERACT_BLOCK)) {
        return false;
      }
      var block = PlatformAdapter.fromFabricWorld(player.getLevel()).blockAt(pos.getX(), pos.getY(), pos.getZ());
      TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
    }
    return true;
  }

  private boolean onBlockBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
    if (level.isClientSide || disabledWorld(player)) {
      return true;
    }
    if (blockEntity instanceof Lockable lockable) {
      var block = PlatformAdapter.fromFabricWorld((ServerLevel) level).blockAt(pos.getX(), pos.getY(), pos.getZ());
      return !handleLockedContainer((ServerPlayer) player, block, lockable);
    }
    return true;
  }

  private boolean handleLockedContainer(ServerPlayer player, Block block, Lockable lockable) {
    if (!Locksmithing.canBreak(PlatformAdapter.fromFabricEntity(player), lockable)) {
      player.sendActionBar(Component.translatable("container.isLocked")
        .args(((Nameable) lockable).getName().asComponent()));
      Sound.BLOCK_CHEST_LOCKED.asEffect().play(block);
      return true;
    }
    return false;
  }

  private void onAfterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
    if (!level.isClientSide && !disabledWorld(player)) {
      var block = PlatformAdapter.fromFabricWorld((ServerLevel) level).blockAt(pos.getX(), pos.getY(), pos.getZ());
      TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
    }
  }

  private InteractionResultHolder<List<ItemStack>> onBlockDropLoot(ServerLevel level, BlockPos pos, List<ItemStack> dropStacks) {
    if (!disabledWorld(level)) {
      var block = PlatformAdapter.fromFabricWorld(level).blockAt(pos.getX(), pos.getY(), pos.getZ());
      if (TempBlock.MANAGER.isTemp(block)) {
        return InteractionResultHolder.fail(dropStacks);
      }
    }
    return InteractionResultHolder.pass(dropStacks);
  }

  private boolean onBlockChange(ServerLevel level, BlockPos pos) {
    if (!disabledWorld(level)) {
      var block = PlatformAdapter.fromFabricWorld(level).blockAt(pos.getX(), pos.getY(), pos.getZ());
      return !TempBlock.MANAGER.isTemp(block);
    }
    return true;
  }

  private boolean onBlockSpread(ServerLevel level, BlockPos pos, BlockPos pos2) {
    if (!disabledWorld(level)) {
      var world = PlatformAdapter.fromFabricWorld(level);
      var block1 = world.blockAt(pos.getX(), pos.getY(), pos.getZ());
      var block2 = world.blockAt(pos2.getX(), pos2.getY(), pos2.getZ());
      return !TempBlock.MANAGER.isTemp(block1) && !TempBlock.MANAGER.isTemp(block2);
    }
    return true;
  }

  private boolean onBlockPistonEvent(ServerLevel level, BlockPos pos, List<BlockPos> toMove, List<BlockPos> toDestroy) {
    if (!disabledWorld(level)) {
      var world = PlatformAdapter.fromFabricWorld(level);
      for (BlockPos bp : toMove) {
        if (TempBlock.MANAGER.isTemp(world.blockAt(bp.getX(), bp.getY(), bp.getZ()))) {
          return false;
        }
      }
      for (BlockPos bp : toDestroy) {
        if (TempBlock.MANAGER.isTemp(world.blockAt(bp.getX(), bp.getY(), bp.getZ()))) {
          return false;
        }
      }
    }
    return true;
  }
}
