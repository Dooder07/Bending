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

package me.moros.bending.model.registry.exception;

/**
 * This exception may be thrown by methods that have detected illegal modification of a mutable
 * registry that has been locked.
 */
public class RegistryModificationException extends RuntimeException {
  /**
   * Constructs a {@code RegistryModificationException} with the specified detail message.
   * @param message the detail message pertaining to this exception.
   */
  public RegistryModificationException(String message) {
    super(message);
  }
}
