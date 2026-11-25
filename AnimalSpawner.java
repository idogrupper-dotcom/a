package il.tiktokzoo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public final class AnimalSpawner {

    private static final Random RNG = new Random();

    /** דרגה: מינימום מטבעות, חיות אפשריות, גודל, בונוס חיים, אפקטים */
    private record Tier(int minCoins, EntityType<?>[] animals, double scale, double extraHealth,
                        boolean glow, boolean buffs, String color) {}

    // מהנמוך לגבוה. coins=0 -> עוקב חדש
    private static final Tier[] TIERS = {
        new Tier(0,    new EntityType<?>[]{EntityType.CHICKEN, EntityType.RABBIT},            1.0, 0,   false, false, "§f"),
        new Tier(1,    new EntityType<?>[]{EntityType.SHEEP, EntityType.PIG, EntityType.COW}, 1.0, 0,   false, false, "§a"),
        new Tier(10,   new EntityType<?>[]{EntityType.FOX, EntityType.CAT, EntityType.WOLF},  1.2, 10,  true,  false, "§b"),
        new Tier(50,   new EntityType<?>[]{EntityType.HORSE, EntityType.PANDA, EntityType.LLAMA}, 1.4, 20, true, true, "§d"),
        new Tier(200,  new EntityType<?>[]{EntityType.POLAR_BEAR, EntityType.CAMEL},          1.8, 40,  true,  true,  "§6"),
        new Tier(1000, new EntityType<?>[]{EntityType.IRON_GOLEM, EntityType.RAVAGER},        2.2, 100, true,  true,  "§c"),
        new Tier(5000, new EntityType<?>[]{EntityType.WARDEN},                                2.0, 200, true,  true,  "§4§l"),
    };

    private static Tier pickTier(int coins) {
        Tier chosen = TIERS[0];
        for (Tier t : TIERS) if (coins >= t.minCoins) chosen = t;
        return chosen;
    }

    /** בטוח לקרוא מכל ת'רד. coins = 0 עבור עוקב. */
    public static void spawn(MinecraftServer server, String playerName, String viewer, int coins) {
        server.execute(() -> {
            ServerPlayer player = playerName != null
                ? server.getPlayerList().getPlayerByName(playerName)
                : null;
            if (player == null && !server.getPlayerList().getPlayers().isEmpty()) {
                player = server.getPlayerList().getPlayers().get(0);
            }
            if (player == null) return;

            ServerLevel level = (ServerLevel) player.level();
            Tier tier = pickTier(coins);
            EntityType<?> type = tier.animals[RNG.nextInt(tier.animals.length)];

            BlockPos around = player.blockPosition().offset(RNG.nextInt(9) - 4, 0, RNG.nextInt(9) - 4);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around);

            Entity e = type.spawn(level, pos, EntitySpawnReason.COMMAND);
            if (e == null) return;

            String label = coins == 0 ? "עוקב" : coins + " מטבעות";
            e.setCustomName(Component.literal(tier.color + viewer + " §7(" + label + ")"));
            e.setCustomNameVisible(true);
            if (tier.glow) e.setGlowingTag(true);

            if (e instanceof LivingEntity le) {
                if (le.getAttribute(Attributes.SCALE) != null) {
                    le.getAttribute(Attributes.SCALE).setBaseValue(tier.scale);
                }
                if (tier.extraHealth > 0 && le.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = le.getMaxHealth() + tier.extraHealth;
                    le.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    le.setHealth((float) hp);
                }
                if (tier.buffs) {
                    le.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 1));
                    le.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60, 1));
                }
            }
        });
    }

    private AnimalSpawner() {}
}
