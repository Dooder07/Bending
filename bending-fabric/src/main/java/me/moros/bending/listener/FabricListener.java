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

import me.moros.bending.model.manager.Game;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

interface FabricListener {
  Game game();

  default boolean disabledWorld(Entity entity) {
    if (entity.getLevel() instanceof ServerLevel world) {
      return disabledWorld(world);
    }
    return true;
  }

  default boolean disabledWorld(ServerLevel world) {
    return !game().worldManager().isEnabled(world.dimension().location());
  }
}
