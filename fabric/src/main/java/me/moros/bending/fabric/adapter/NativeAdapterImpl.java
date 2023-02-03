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

package me.moros.bending.fabric.adapter;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.common.adapter.AbstractNativeAdapter;
import me.moros.bending.fabric.mixin.accessor.EntityAccess;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.block.FabricBlockState;
import me.moros.bending.fabric.platform.entity.FabricEntity;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import me.moros.bending.fabric.platform.world.FabricWorld;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class NativeAdapterImpl extends AbstractNativeAdapter {
  private final FabricServerAudiences audiences;

  public NativeAdapterImpl(MinecraftServer server) {
    super(server.getPlayerList());
    this.audiences = FabricServerAudiences.of(server);
  }

  @Override
  protected ServerLevel adapt(World world) {
    return ((FabricWorld) world).handle();
  }

  @Override
  protected BlockState adapt(me.moros.bending.api.platform.block.BlockState state) {
    return ((FabricBlockState) state).handle();
  }

  @Override
  protected ServerPlayer adapt(Player player) {
    return ((FabricPlayer) player).handle();
  }

  @Override
  protected net.minecraft.world.entity.Entity adapt(Entity entity) {
    return ((FabricEntity) entity).handle();
  }

  @Override
  protected ItemStack adapt(Item item) {
    return BuiltInRegistries.ITEM.get(PlatformAdapter.rsl(item.key())).getDefaultInstance();
  }

  @Override
  protected net.minecraft.network.chat.Component adapt(Component component) {
    return audiences.toNative(component);
  }

  @Override
  protected int nextEntityId() {
    return EntityAccess.idCounter().incrementAndGet();
  }
}
