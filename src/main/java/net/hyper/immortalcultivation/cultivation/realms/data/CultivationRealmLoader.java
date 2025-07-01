package net.hyper.immortalcultivation.cultivation.realms.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads cultivation realm data from JSON files in data packs.
 * JSON files should be placed in data/[namespace]/cultivation_realms/
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class CultivationRealmLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "cultivation_realms";

    private static CultivationRealmLoader instance;
    private final Map<String, CultivationRealm> loadedRealms = new HashMap<>();

    public CultivationRealmLoader() {
        super(GSON, FOLDER);
        instance = this;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        loadedRealms.clear();

        jsons.forEach((location, json) -> {
            try {
                // Parse the JSON using the codec
                CultivationRealmData.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error(
                        "Error parsing cultivation realm {}: {}", location, error))
                    .ifPresent(data -> {
                        // Create the realm instance
                        JsonCultivationRealm realm = new JsonCultivationRealm(data);
                        loadedRealms.put(data.id(), realm);

                        ImmortalCultivation.LOGGER.info("Loaded cultivation realm: {} (Level {})",
                            data.displayName(), data.realmLevel());
                    });
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Failed to load cultivation realm from {}", location, e);
            }
        });

        ImmortalCultivation.LOGGER.info("Loaded {} cultivation realms from data packs", loadedRealms.size());

        // Update the main registry with loaded realms
        updateMainRegistry();
    }

    /**
     * Updates the main cultivation realm registry with loaded realms.
     * This allows existing code to continue working with the static registry.
     */
    private void updateMainRegistry() {
        // You'll need to modify ICModCultivationRealms to support dynamic registration
        // For now, this is a placeholder
        loadedRealms.forEach((id, realm) -> {
            // ICModCultivationRealms.registerDynamic(id, realm);
        });
    }

    /**
     * Gets a loaded realm by ID
     */
    public static CultivationRealm getRealm(String id) {
        if (instance == null) return null;
        return instance.loadedRealms.get(id);
    }

    /**
     * Gets all loaded realms
     */
    public static Map<String, CultivationRealm> getAllRealms() {
        if (instance == null) return new HashMap<>();
        return new HashMap<>(instance.loadedRealms);
    }

    // === Event Handlers ===

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CultivationRealmLoader());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ImmortalCultivation.LOGGER.info("Cultivation realm data will be loaded from data packs");
    }
}