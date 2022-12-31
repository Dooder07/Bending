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

package me.moros.bending.model.raytrace;

import java.util.Objects;

import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.Entity;
import me.moros.math.Vector3d;

public interface CompositeRayTrace extends EntityRayTrace, BlockRayTrace {
  static CompositeRayTrace miss(Vector3d position) {
    Objects.requireNonNull(position);
    return new CompositeRayTraceImpl(position, null, null);
  }

  static CompositeRayTrace hit(Vector3d position, Block block) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(block);
    return new CompositeRayTraceImpl(position, block, null);
  }

  static CompositeRayTrace hit(Vector3d position, Entity entity) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(entity);
    return new CompositeRayTraceImpl(position, null, entity);
  }
}
