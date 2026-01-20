package indi.etern.musichud.bukkit.commands;

import indi.etern.musichud.bukkit.MusicHudPlugin;
import indi.etern.musichud.bukkit.beans.MusicDetail;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Main command handler for MusicHud plugin.
 */
public class MusicHudCommand implements CommandExecutor, TabCompleter {
    private final MusicHudPlugin plugin;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "status", "queue", "skip", "reload", "start", "stop", "help"
    );
    
    public MusicHudCommand(MusicHudPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status" -> showStatus(sender);
            case "queue" -> showQueue(sender);
            case "skip" -> forceSkip(sender);
            case "reload" -> reloadConfig(sender);
            case "start" -> startService(sender);
            case "stop" -> stopService(sender);
            case "help" -> showHelp(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "未知命令: " + subCommand);
                showHelp(sender);
            }
        }
        
        return true;
    }
    
    private void showStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== MusicHud 状态 =====");
        
        MusicDetail current = plugin.getMusicPlayerService().getCurrentMusicDetail();
        if (current != null && !current.equals(MusicDetail.NONE)) {
            sender.sendMessage(ChatColor.GREEN + "当前播放: " + ChatColor.WHITE + current.getName());
            sender.sendMessage(ChatColor.GREEN + "艺术家: " + ChatColor.WHITE + current.getArtistNames());
            if (current.getPusherInfo() != null && !current.getPusherInfo().playerName().isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "点歌人: " + ChatColor.WHITE + current.getPusherInfo().playerName());
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "当前没有正在播放的音乐");
        }
        
        int queueSize = plugin.getMusicPlayerService().getMusicQueue().size();
        sender.sendMessage(ChatColor.GREEN + "队列长度: " + ChatColor.WHITE + queueSize);
        
        int connectedPlayers = plugin.getLoginService().getConnectedPlayers().size();
        sender.sendMessage(ChatColor.GREEN + "已连接玩家: " + ChatColor.WHITE + connectedPlayers);
    }
    
    private void showQueue(CommandSender sender) {
        Queue<MusicDetail> queue = plugin.getMusicPlayerService().getMusicQueue();
        
        if (queue.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "播放队列为空");
            return;
        }
        
        sender.sendMessage(ChatColor.GOLD + "===== 播放队列 =====");
        int index = 1;
        for (MusicDetail music : queue) {
            String pusher = music.getPusherInfo() != null && !music.getPusherInfo().playerName().isEmpty() 
                    ? " (" + music.getPusherInfo().playerName() + ")" 
                    : "";
            sender.sendMessage(ChatColor.WHITE + String.valueOf(index++) + ". " + 
                    ChatColor.GREEN + music.getName() + 
                    ChatColor.GRAY + " - " + music.getArtistNames() + 
                    ChatColor.AQUA + pusher);
        }
    }
    
    private void forceSkip(CommandSender sender) {
        if (!sender.hasPermission("musichud.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }
        
        MusicDetail current = plugin.getMusicPlayerService().getCurrentMusicDetail();
        if (current == null || current.equals(MusicDetail.NONE)) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有正在播放的音乐");
            return;
        }
        
        // Force skip by interrupting the pusher thread
        plugin.getMusicPlayerService().voteSkipCurrent(current.getId(), 
                sender instanceof Player ? (Player) sender : null);
        
        sender.sendMessage(ChatColor.GREEN + "已强制跳过当前音乐");
    }
    
    private void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("musichud.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }
        
        plugin.reloadConfig();
        plugin.getPluginConfig().reload();
        
        sender.sendMessage(ChatColor.GREEN + "配置已重新加载");
    }
    
    private void startService(CommandSender sender) {
        if (!sender.hasPermission("musichud.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }
        
        plugin.getMusicPlayerService().start();
        sender.sendMessage(ChatColor.GREEN + "音乐播放服务已启动");
    }
    
    private void stopService(CommandSender sender) {
        if (!sender.hasPermission("musichud.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }
        
        plugin.getMusicPlayerService().shutdown();
        sender.sendMessage(ChatColor.GREEN + "音乐播放服务已停止");
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== MusicHud 命令帮助 =====");
        sender.sendMessage(ChatColor.GREEN + "/musichud status" + ChatColor.WHITE + " - 显示当前播放状态");
        sender.sendMessage(ChatColor.GREEN + "/musichud queue" + ChatColor.WHITE + " - 显示播放队列");
        sender.sendMessage(ChatColor.GREEN + "/musichud skip" + ChatColor.WHITE + " - 强制跳过当前音乐 (需要管理员权限)");
        sender.sendMessage(ChatColor.GREEN + "/musichud start" + ChatColor.WHITE + " - 启动音乐服务 (需要管理员权限)");
        sender.sendMessage(ChatColor.GREEN + "/musichud stop" + ChatColor.WHITE + " - 停止音乐服务 (需要管理员权限)");
        sender.sendMessage(ChatColor.GREEN + "/musichud reload" + ChatColor.WHITE + " - 重新加载配置 (需要管理员权限)");
        sender.sendMessage(ChatColor.GREEN + "/musichud help" + ChatColor.WHITE + " - 显示此帮助信息");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
