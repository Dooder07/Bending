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

package me.moros.bending.common.command.commands;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.meta.CommandMeta;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;

public record BoardCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder();
    commander().register(builder.literal("board")
      .meta(CommandMeta.DESCRIPTION, "Toggle bending board visibility")
      .permission(CommandPermissions.BOARD)
      .senderType(commander().playerType())
      .handler(c -> onBoard(c.get(ContextKeys.BENDING_PLAYER)))
    );
  }

  private void onBoard(BendingPlayer player) {
    boolean hidden = player.store().has(Board.HIDDEN);
    if (!player.board().isEnabled() && !hidden) {
      Message.BOARD_DISABLED.send(player);
      return;
    }
    if (hidden) {
      player.store().remove(Board.HIDDEN);
      Message.BOARD_TOGGLED_ON.send(player);
    } else {
      player.store().add(Board.HIDDEN, Board.dummy());
      Message.BOARD_TOGGLED_OFF.send(player);
    }
    player.board();
  }
}