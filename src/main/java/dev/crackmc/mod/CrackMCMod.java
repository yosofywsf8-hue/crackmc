package dev.crackmc.mod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.crackmc.mod.tunnel.TunnelManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CrackMCMod implements ModInitializer {

    public static final String MOD_ID = "crackmc";
    public static final Logger LOGGER = LoggerFactory.getLogger("CrackMC");

    public static volatile boolean crackedModeEnabled = true;
    public static volatile String hostUsername = null;
    public static volatile String pinCode = null;
    public static volatile int maxPlayers = 8;
    public static final Set<String> bannedNames = new HashSet<>();
    public static final Set<String> verifiedPinPlayers = new HashSet<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[CrackMC] Initialized (1.21.1 - 26.3 compatibility mode).");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("crackmc")
                    .then(CommandManager.literal("share")
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player != null) {
                                    hostUsername = player.getName().getString();
                                }
                                crackedModeEnabled = true;
                                int localPort = ctx.getSource().getServer().getServerPort();
                                if (localPort <= 0) {
                                    localPort = 25565;
                                }
                                int finalPort = localPort;
                                ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Opening cracked LAN & starting tunnel on port " + finalPort + "...")
                                        .formatted(Formatting.AQUA), false);

                                TunnelManager.startTunnel(finalPort, address -> {
                                    ctx.getSource().getServer().execute(() -> {
                                        MutableText msg = Text.literal("[CrackMC] Share this link with friends: ")
                                                .formatted(Formatting.GREEN)
                                                .append(Text.literal(address)
                                                        .formatted(Formatting.YELLOW, Formatting.UNDERLINE)
                                                        .styled(style -> style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, address))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to copy address")))));
                                        ctx.getSource().sendFeedback(() -> msg, false);
                                    });
                                }, err -> ctx.getSource().getServer().execute(() ->
                                        ctx.getSource().sendError(Text.literal("[CrackMC] Tunnel error: " + err))));
                                return 1;
                            }))
                    .then(CommandManager.literal("stop")
                            .executes(ctx -> {
                                TunnelManager.stopTunnel();
                                ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Tunnel stopped.")
                                        .formatted(Formatting.RED), false);
                                return 1;
                            }))
                    .then(CommandManager.literal("max")
                            .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                                    .executes(ctx -> {
                                        maxPlayers = IntegerArgumentType.getInteger(ctx, "count");
                                        ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Max players set to " + maxPlayers)
                                                .formatted(Formatting.GREEN), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("pin")
                            .then(CommandManager.literal("off")
                                    .executes(ctx -> {
                                        pinCode = null;
                                        verifiedPinPlayers.clear();
                                        ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] PIN disabled.")
                                                .formatted(Formatting.YELLOW), false);
                                        return 1;
                                    }))
                            .then(CommandManager.argument("code", StringArgumentType.word())
                                    .executes(ctx -> {
                                        pinCode = StringArgumentType.getString(ctx, "code");
                                        verifiedPinPlayers.clear();
                                        ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] World PIN enabled: " + pinCode)
                                                .formatted(Formatting.GREEN), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("kick")
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                        target.networkHandler.disconnect(Text.literal("Kicked by CrackMC host."));
                                        ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Kicked " + target.getName().getString())
                                                .formatted(Formatting.GOLD), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("ban")
                            .then(CommandManager.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
                                        bannedNames.add(name);
                                        ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Banned " + name)
                                                .formatted(Formatting.RED), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("info")
                            .executes(ctx -> {
                                String status = TunnelManager.isRunning()
                                        ? "ACTIVE (" + TunnelManager.getPublicAddress() + ")"
                                        : "OFFLINE";
                                ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] Tunnel: " + status
                                        + " | Cracked: " + crackedModeEnabled
                                        + " | Max: " + maxPlayers
                                        + " | PIN: " + (pinCode != null ? "ON" : "OFF"))
                                        .formatted(Formatting.LIGHT_PURPLE), false);
                                return 1;
                            }))
            );

            dispatcher.register(CommandManager.literal("pin")
                    .then(CommandManager.argument("code", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                if (pinCode == null || pinCode.equals(StringArgumentType.getString(ctx, "code"))) {
                                    verifiedPinPlayers.add(player.getName().getString().toLowerCase(Locale.ROOT));
                                    ctx.getSource().sendFeedback(() -> Text.literal("[CrackMC] PIN verified! Welcome.")
                                            .formatted(Formatting.GREEN), false);
                                } else {
                                    player.networkHandler.disconnect(Text.literal("Invalid CrackMC PIN."));
                                }
                                return 1;
                            })));
        });
    }
}
