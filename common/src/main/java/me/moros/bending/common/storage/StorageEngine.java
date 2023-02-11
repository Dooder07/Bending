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

package me.moros.bending.common.storage;

import java.util.Optional;
import java.util.function.Supplier;

import me.moros.bending.common.storage.file.loader.HoconLoader;
import me.moros.bending.common.storage.file.loader.JsonLoader;
import me.moros.bending.common.storage.file.loader.Loader;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum StorageEngine {
  // Flat file
  JSON("JSON", JsonLoader::new),
  HOCON("HOCON", HoconLoader::new),

  // Remote databases
  MYSQL(StorageType.MYSQL),
  MARIADB(StorageType.MARIADB),
  POSTGRESQL(StorageType.POSTGRESQL),
  // Local databases
  SQLITE(StorageType.SQLITE),
  H2(StorageType.H2),
  HSQL(StorageType.HSQL);

  private final String name;
  private final StorageType type;
  private final Supplier<Loader<?>> loaderSupplier;

  StorageEngine(StorageType type) {
    this(type.toString(), type, () -> null);
  }

  StorageEngine(String name, Supplier<Loader<?>> loaderSupplier) {
    this(name, null, loaderSupplier);
  }

  StorageEngine(String name, @Nullable StorageType type, Supplier<@Nullable Loader<?>> loaderSupplier) {
    this.name = name;
    this.type = type;
    this.loaderSupplier = loaderSupplier;
  }

  @Override
  public String toString() {
    return name;
  }

  public Optional<StorageType> type() {
    return Optional.ofNullable(type);
  }

  public Optional<Loader<?>> loader() {
    return Optional.ofNullable(loaderSupplier.get());
  }
}
