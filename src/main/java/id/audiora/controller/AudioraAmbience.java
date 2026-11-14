package id.audiora.controller;

import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class AudioraAmbience {
    private static final Identifier BIRDS_ID = Identifier.fromNamespaceAndPath("audiora", "birds_forest");
    private static final Identifier CRICKETS_ID = Identifier.fromNamespaceAndPath("audiora", "crickets_night");
    private static final SoundEvent BIRDS = Registry.register(
        BuiltInRegistries.SOUND_EVENT, BIRDS_ID, SoundEvent.createVariableRangeEvent(BIRDS_ID)
    );
    private static final SoundEvent CRICKETS = Registry.register(
        BuiltInRegistries.SOUND_EVENT, CRICKETS_ID, SoundEvent.createVariableRangeEvent(CRICKETS_ID)
    );
    private static final Random RANDOM = new Random();
    private static int ticksUntilNext = 20 * 18;

    private AudioraAmbience() {}

    public static void initialize() {
        // Force sound-event registration during client initialization, before the first tick.
    }

    public static void tick(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) return;
        if (--ticksUntilNext > 0) return;

        String biome = client.level.getBiome(client.player.blockPosition())
            .unwrapKey().map(key -> key.identifier().getPath()).orElse("");
        long dayTime = Math.floorMod(client.level.getGameTime(), 24000L);
        boolean night = dayTime >= 13000L && dayTime <= 23000L;
        boolean forest = biome.contains("forest") || biome.contains("jungle") ||
            biome.contains("grove") || biome.contains("taiga");

        if (night && AudioraConfig.enabled[AudioraConfig.indexOfCategory("insects")]) {
            float scale = AudioraConfig.scaleForSound("audiora:crickets_night");
            if (scale > 0.0f) {
                client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                    CRICKETS, SoundSource.AMBIENT, 0.75f, 0.94f + RANDOM.nextFloat() * 0.12f, false);
            }
        } else if (forest && AudioraConfig.enabled[AudioraConfig.indexOfCategory("birds")]) {
            float scale = AudioraConfig.scaleForSound("audiora:birds_forest");
            if (scale > 0.0f) {
                client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                    BIRDS, SoundSource.AMBIENT, 0.75f, 0.94f + RANDOM.nextFloat() * 0.12f, false);
            }
        }
        ticksUntilNext = 20 * (25 + RANDOM.nextInt(31));
    }
}
