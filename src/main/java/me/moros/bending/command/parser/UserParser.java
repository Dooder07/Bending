/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.command.parser;

import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.parsers.PlayerArgument.PlayerParseException;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class UserParser implements ArgumentParser<CommandSender, User> {
  @Override
  public @NonNull ArgumentParseResult<User> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
    String input = inputQueue.peek();
    if (input == null) {
      return ArgumentParseResult.failure(new NoInputProvidedException(UserParser.class, commandContext));
    }
    inputQueue.remove();
    if (input.equalsIgnoreCase("me") && commandContext.getSender() instanceof Player player) {
      return ArgumentParseResult.success(Registries.BENDERS.user(player));
    }
    Player player = Bukkit.getPlayer(input);
    if (player == null) {
      return ArgumentParseResult.failure(new PlayerParseException(input, commandContext));
    }
    return ArgumentParseResult.success(Registries.BENDERS.user(player));
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<CommandSender> commandContext, final @NonNull String input) {
    Predicate<BendingPlayer> canSee;
    if (commandContext.getSender() instanceof Player sender) {
      canSee = p -> sender.canSee(p.entity());
    } else {
      canSee = x -> true;
    }
    return Registries.BENDERS.players().filter(canSee).map(p -> p.entity().getName()).toList();
  }
}
