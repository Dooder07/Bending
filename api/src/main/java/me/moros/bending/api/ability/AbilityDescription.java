/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.translation.Translatable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * AbilityDescription is immutable and thread-safe.
 * Assume that all collections returning AbilityDescription are also immutable
 */
public sealed class AbilityDescription implements Keyed, Translatable permits AbilityDescription.Sequence {
  private final Key key;
  private final String name;
  private final Element element;
  private final Component displayName;
  private final Function<AbilityDescription, ? extends Ability> constructor;
  private final EnumSet<Activation> activations;
  private final Collection<String> requiredPermissions;
  private final boolean canBind;
  private final boolean hidden;
  private final boolean bypassCooldown;
  private final int hashcode;

  private AbilityDescription(Builder builder) {
    key = builder.key;
    name = builder.name;
    element = builder.element;
    displayName = Component.text(name, element.color());
    constructor = builder.constructor;
    activations = builder.activations;
    requiredPermissions = List.copyOf(builder.requiredPermissions);
    canBind = builder.canBind && !isActivatedBy(Activation.SEQUENCE);
    hidden = builder.hidden;
    bypassCooldown = builder.bypassCooldown;
    hashcode = Objects.hash(key, element, activations);
  }

  @Deprecated(forRemoval = true)
  public String name() {
    return name;
  }

  public Component displayName() {
    return displayName;
  }

  public Element element() {
    return element;
  }

  public boolean canBind() {
    return canBind;
  }

  public boolean hidden() {
    return hidden;
  }

  public boolean bypassCooldown() {
    return bypassCooldown;
  }

  public boolean isActivatedBy(Activation method) {
    return activations.contains(method);
  }

  public Ability createAbility() {
    return constructor.apply(this);
  }

  public Collection<String> permissions() {
    return requiredPermissions;
  }

  @Deprecated(forRemoval = true)
  public Component meta() {
    return displayName().clickEvent(ClickEvent.runCommand("/bending help " + key().asString()));
  }

  @Override
  public Key key() {
    return key;
  }

  @Override
  public String translationKey() {
    return key().namespace() + ".ability." + key().value();
  }

  @Deprecated(forRemoval = true)
  public String descriptionKey() {
    return translationKey() + ".description";
  }

  @Deprecated(forRemoval = true)
  public String instructionsKey() {
    return translationKey() + ".instructions";
  }

  @Deprecated(forRemoval = true)
  public String deathKey() {
    return translationKey() + ".death";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbilityDescription other = (AbilityDescription) obj;
    return key.equals(other.key) && element == other.element && activations.equals(other.activations);
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  public static <T extends Ability> Builder builder(String name, Function<AbilityDescription, T> constructor) {
    return builder(KeyUtil.BENDING_NAMESPACE, name, constructor);
  }

  public static <T extends Ability> Builder builder(String namespace, String name, Function<AbilityDescription, T> constructor) {
    Objects.requireNonNull(namespace);
    Objects.requireNonNull(name);
    Objects.requireNonNull(constructor);
    if (namespace.isEmpty()) {
      namespace = KeyUtil.BENDING_NAMESPACE;
    }
    boolean validName = name.chars().allMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    if (name.isEmpty() || !validName) {
      throw new IllegalArgumentException("Name must be an alphabetical non-empty string!");
    }
    return new Builder(namespace, name, constructor);
  }

  /**
   * Immutable and thread-safe representation of a sequence
   */
  public static final class Sequence extends AbilityDescription {
    public static final int MAX_STEPS = 16;

    private final List<SequenceStep> steps;
    private Component instructions;

    private Sequence(Builder builder, List<SequenceStep> steps) {
      super(builder);
      this.steps = List.copyOf(steps);
    }

    /**
     * Get the steps required to activate this sequence.
     * @return an immutable collection of this sequence's steps
     */
    public List<SequenceStep> steps() {
      return steps;
    }

    /**
     * Get the instructions for this sequence.
     * @return the instructions
     */
    public Component instructions() {
      if (instructions == null) {
        instructions = generateInstructions();
      }
      return instructions;
    }

    private Component generateInstructions() {
      TextComponent.Builder builder = Component.text();
      int size = steps.size();
      for (int i = 0; i < size; i++) {
        SequenceStep sequenceStep = steps.get(i);
        if (i != 0) {
          builder.append(Component.text(" > "));
        }
        AbilityDescription desc = sequenceStep.ability();
        Activation action = sequenceStep.activation();
        String key = action.translationKey();
        if (action == Activation.SNEAK && i + 1 < steps.size()) {
          // Check if the next instruction is to release sneak.
          SequenceStep next = steps.get(i + 1);
          if (desc.equals(next.ability()) && next.activation() == Activation.SNEAK_RELEASE) {
            key = "bending.activation.sneak-tap";
            i++;
          }
        }
        builder.append(desc.displayName()).append(Component.text(" ("))
          .append(Component.translatable(key)).append(Component.text(")"));
      }
      return builder.build();
    }

    /**
     * Check if this sequence can be activated by the provided sequence steps.
     * This method will try to match the sequence steps and fail-fast.
     * @param otherSteps the steps to match
     * @return true if this sequence can be activated by the given steps, false otherwise
     */
    public boolean matches(List<SequenceStep> otherSteps) {
      int actionsLength = otherSteps.size() - 1;
      int sequenceLength = steps.size() - 1;
      if (actionsLength < sequenceLength) {
        return false;
      }
      for (int i = 0; i <= sequenceLength; i++) {
        SequenceStep first = steps.get(sequenceLength - i);
        SequenceStep second = otherSteps.get(actionsLength - i);
        if (!first.equals(second)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Builder to create {@link AbilityDescription}.
   */
  public static final class Builder {
    private final Key key;
    private final String name;
    private Element element;
    private final Function<AbilityDescription, ? extends Ability> constructor;
    private EnumSet<Activation> activations;
    private Collection<String> requiredPermissions;
    private boolean canBind = true;
    private boolean hidden = false;
    private boolean bypassCooldown = false;

    private <T extends Ability> Builder(String namespace, String name, Function<AbilityDescription, T> constructor) {
      this.key = Key.key(namespace, name.toLowerCase(Locale.ROOT));
      this.name = name;
      this.constructor = constructor;
      this.requiredPermissions = List.of(defaultPermission());
    }

    public Builder element(Element element) {
      this.element = element;
      return this;
    }

    public Builder activation(Activation method, Activation @Nullable ... methods) {
      Collection<Activation> c = new ArrayList<>();
      if (methods != null) {
        c.addAll(List.of(methods));
      }
      c.add(method);
      activations = EnumSet.copyOf(c);
      return this;
    }

    public Builder require(String @Nullable ... permissions) {
      Collection<String> c = new ArrayList<>();
      c.add(defaultPermission());
      if (permissions != null) {
        c.addAll(List.of(permissions));
      }
      requiredPermissions = c;
      return this;
    }

    public Builder canBind(boolean canBind) {
      this.canBind = canBind;
      return this;
    }

    public Builder hidden(boolean hidden) {
      this.hidden = hidden;
      return this;
    }

    public Builder bypassCooldown(boolean bypassCooldown) {
      this.bypassCooldown = bypassCooldown;
      return this;
    }

    public AbilityDescription build() {
      validate();
      if (activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Can't build sequence");
      }
      return new AbilityDescription(this);
    }

    public Sequence buildSequence(UnaryOperator<SequenceBuilder> function) {
      validate();
      if (!activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Ability must be activated by sequence");
      }
      List<SequenceStep> sequenceSteps = function.apply(new SequenceBuilder()).validateAndBuild();
      return new Sequence(this, sequenceSteps);
    }

    /**
     * @deprecated use {@link #buildSequence(UnaryOperator)} instead
     */
    @Deprecated(forRemoval = true)
    public Sequence buildSequence(SequenceStep step1, SequenceStep step2, SequenceStep @Nullable ... steps) {
      validate();
      if (!activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Ability must be activated by sequence");
      }
      List<SequenceStep> sequenceSteps = new ArrayList<>();
      sequenceSteps.add(Objects.requireNonNull(step1, "Sequence steps cannot be null"));
      sequenceSteps.add(Objects.requireNonNull(step2, "Sequence steps cannot be null"));
      if (steps != null) {
        sequenceSteps.addAll(List.of(steps));
      }
      return new Sequence(this, sequenceSteps);
    }

    private void validate() {
      Objects.requireNonNull(element, "Element cannot be null");
      Objects.requireNonNull(activations, "Activations cannot be null");
      if (activations.isEmpty()) {
        throw new IllegalStateException("Activation methods cannot be empty");
      }
    }

    private String defaultPermission() {
      return "bending.ability." + key.value();
    }
  }
}
