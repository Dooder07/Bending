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

package me.moros.bending.event;

import java.util.Collection;
import java.util.List;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BendingExplosionEvent extends EntityExplodeEvent {
  private final User user;

  BendingExplosionEvent(User user, Vector3d location, Collection<Block> blocks, float yield) {
    super(user.entity(), location.toLocation(user.world()), List.copyOf(blocks), yield);
    this.user = user;
  }

  public @NonNull User user() {
    return user;
  }
}