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

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import me.moros.bending.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable and thread-safe object that represents a bending element
 */
public enum Element {
  AIR("Air", ColorPalette.AIR),
  WATER("Water", ColorPalette.WATER),
  EARTH("Earth", ColorPalette.EARTH),
  FIRE("Fire", ColorPalette.FIRE);

  private final String elementName;
  private final TextColor color;

  Element(String elementName, TextColor color) {
    this.elementName = elementName;
    this.color = color;
  }

  @Override
  public String toString() {
    return elementName;
  }

  private String key() {
    return "bending.element." + elementName.toLowerCase(Locale.ROOT);
  }

  public Component displayName() {
    return Component.translatable(key(), color);
  }

  public Component description() {
    return Component.translatable(key() + ".description", color);
  }

  public TextColor color() {
    return color;
  }

  public static Optional<Element> fromName(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }
    return VALUES.stream().filter(e -> e.name().startsWith(value.toUpperCase(Locale.ROOT))).findAny();
  }

  public static final Collection<Element> VALUES = List.of(values());

  public static final Collection<String> NAMES = List.of("Air", "Water", "Earth", "Fire");
}
