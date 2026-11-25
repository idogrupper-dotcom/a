package il.tiktokzoo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class TikTokZoo implements ModInitializer {

    private static volatile LiveClient client;
    private static volatile String targetPlayer; // השחקן שסביבו החיות מופיעות

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerTikTok(dispatcher);
            registerTest(dispatcher);
        });
    }

    private static void registerTikTok(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tiktok")
            .then(Commands.literal("connect")
                .then(Commands.argument("username", StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        targetPlayer = player.getGameProfile().name();
                        String user = StringArgumentType.getString(ctx, "username");
                        connect(ctx.getSource().getServer(), user);
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("מתחבר ללייב של @" + user + " ..."), false);
                        return 1;
                    })))
            .then(Commands.literal("disconnect")
                .executes(ctx -> {
                    disconnect();
                    ctx.getSource().sendSuccess(() -> Component.literal("התנתקת מהלייב"), false);
                    return 1;
                })));
    }

    /**
     * פקודות בדיקה (בלי לייב):
     *   /test follow            - מדמה עוקב חדש
     *   /test follow <שם>       - מדמה עוקב בשם מסוים
     *   /test gift <מטבעות>     - מדמה מתנה
     *   /test gift <מטבעות> <שם>
     */
    private static void registerTest(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("test")
            .then(Commands.literal("follow")
                .executes(ctx -> testFollow(ctx.getSource(), "TestFollower"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> testFollow(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("gift")
                .then(Commands.argument("coins", IntegerArgumentType.integer(1))
                    .executes(ctx -> testGift(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "coins"), "TestGifter"))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> testGift(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "coins"),
                            StringArgumentType.getString(ctx, "name")))))));
    }

    private static int testFollow(CommandSourceStack src, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        AnimalSpawner.spawn(src.getServer(), p.getGameProfile().name(), name, 0);
        src.sendSuccess(() -> Component.literal("בדיקה: " + name + " עוקב עכשיו!"), false);
        return 1;
    }

    private static int testGift(CommandSourceStack src, int coins, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        AnimalSpawner.spawn(src.getServer(), p.getGameProfile().name(), name, coins);
        src.sendSuccess(() -> Component.literal("בדיקה: " + name + " שלח מתנה של " + coins + " מטבעות"), false);
        return 1;
    }

    private static synchronized void connect(MinecraftServer server, String username) {
        disconnect();
        Thread t = new Thread(() -> {
            try {
                client = TikTokLive.newClient(username)
                    .onConnected((c, e) -> broadcast(server, "מחובר ללייב של @" + username))
                    .onFollow((c, e) ->
                        AnimalSpawner.spawn(server, targetPlayer, e.getUser().getName(), 0))
                    .onGift((c, e) -> {
                        int coins = (int) e.getGift().getDiamondCost();
                        AnimalSpawner.spawn(server, targetPlayer, e.getUser().getName(), Math.max(coins, 1));
                    })
                    .buildAndConnect();
            } catch (Exception ex) {
                broadcast(server, "שגיאת חיבור ל-TikTok: " + ex.getMessage());
            }
        }, "tiktok-zoo-connect");
        t.setDaemon(true);
        t.start();
    }

    private static synchronized void disconnect() {
        if (client != null) {
            try { client.disconnect(); } catch (Exception ignored) {}
            client = null;
        }
    }

    private static void broadcast(MinecraftServer server, String msg) {
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false));
    }
}
