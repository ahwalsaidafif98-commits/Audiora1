package id.audiora.controller;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class AudioraAmbience {
    private static final Identifier BIRDS_ID = Identifier.fromNamespaceAndPath("audiora", "birds_forest");
    private static final Identifier CRICKETS_ID = Identifier.fromNamespaceAndPath("audiora", "crickets_night");
    private static final Identifier WIND_ID = Identifier.fromNamespaceAndPath("audiora", "open_air_wind");

    private static final SoundEvent BIRDS = register(BIRDS_ID);
    private static final SoundEvent CRICKETS = register(CRICKETS_ID);
    private static final SoundEvent WIND = register(WIND_ID);

    private static final Random RANDOM = new Random();
    private static int ticksUntilNext = 20 * 12;

    private AudioraAmbience() {}

    private static SoundEvent register(Identifier id) {
        return Registry.register(
            BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void initialize() {
        // Force sound-event registration before the client starts playing ambience.
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
        boolean outdoors = client.level.canSeeSky(client.player.blockPosition());

        if (night && AudioraConfig.enabled[AudioraConfig.indexOfCategory("insects")]) {
            play(client, CRICKETS, "audiora:crickets_night", 0.72f);
        } else if (!night && forest && AudioraConfig.enabled[AudioraConfig.indexOfCategory("birds")]) {
            play(client, BIRDS, "audiora:birds_forest", 0.68f);
        }

        if (outdoors && AudioraConfig.enabled[AudioraConfig.indexOfCategory("nature")]) {
            play(client, WIND, "audiora:open_air_wind", 0.42f);
        }

        ticksUntilNext = 20 * (24 + RANDOM.nextInt(22));
    }

    private static void play(net.minecraft.client.Minecraft client, SoundEvent event, String id, float volume) {
        float scale = AudioraConfig.scaleForSound(id);
        if (scale <= 0.0f) return;
        client.level.playLocalSound(
            client.player.getX(), client.player.getY(), client.player.getZ(),
            event, SoundSource.AMBIENT, volume * scale,
            0.94f + RANDOM.nextFloat() * 0.12f, false
        );
    }
}
