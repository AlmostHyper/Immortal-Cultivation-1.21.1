package net.hyper.immortalcultivation.registry;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class ICModKeyMappings {
    public static KeyMapping cultivationKey;

    public static void register(RegisterKeyMappingsEvent event) {
        cultivationKey = new KeyMapping(
            "key.icmod.accelerate_cultivation",  // The true name
            InputConstants.Type.KEYSYM,          // The realm of keys
            GLFW.GLFW_KEY_C,                     // The vessel
            "key.category.icmod"                  // The family
        );

        event.register(cultivationKey);
    }
}
