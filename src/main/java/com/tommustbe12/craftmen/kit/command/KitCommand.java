package com.tommustbe12.craftmen.kit.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.gui.KitEditorMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class KitCommand implements CommandExecutor, TabCompleter {

    private final KitEditorMenu menu;

    public KitCommand(KitEditorMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            menu.openSelect(player);
            return true;
        }

        String gameName = String.join(" ", args);
        Game game = Craftmen.get().getGameManager().getGame(gameName);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Unknown kit/game: " + gameName);
            return true;
        }

        menu.openEdit(player, game, true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();
        String prefix = String.join(" ", args).toLowerCase();

        List<String> names = Craftmen.get().getGameManager().getGames().stream()
                .map(Game::getName)
                .collect(Collectors.toList());

        List<String> out = new ArrayList<>();
        for (String name : names) {
            if (name.toLowerCase().startsWith(prefix)) out.add(name);
        }
        return out;
    }
}

