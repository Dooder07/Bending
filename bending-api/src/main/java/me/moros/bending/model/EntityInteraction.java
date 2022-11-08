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

package me.moros.bending.model;

import java.lang.ref.WeakReference;

import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.math.Vector3d;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

public record EntityInteraction(WeakReference<Entity> entity, @Nullable Vector3d point) {
  public static RegistryKey<EntityInteraction> KEY = RegistryKey.create("last-interacted-enttity", EntityInteraction.class);

  public EntityInteraction(Entity entity, @Nullable Vector3d point) {
    this(new WeakReference<>(entity), point);
  }
}