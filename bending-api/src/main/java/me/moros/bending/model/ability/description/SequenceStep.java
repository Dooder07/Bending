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

package me.moros.bending.model.ability.description;

import java.util.Objects;

import me.moros.bending.model.ability.Activation;

/**
 * Immutable and thread-safe pair representation of {@link AbilityDescription} and {@link Activation}
 */
public final class SequenceStep {
  private final AbilityDescription desc;
  private final Activation action;
  private final int hashcode;

  public SequenceStep(AbilityDescription desc, Activation action) {
    this.desc = desc;
    this.action = action;
    hashcode = Objects.hash(desc, action);
  }

  public AbilityDescription ability() {
    return desc;
  }

  public Activation activation() {
    return action;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SequenceStep otherAction) {
      return action == otherAction.action && desc.equals(otherAction.desc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    return desc.name() + ": " + action.name();
  }
}
