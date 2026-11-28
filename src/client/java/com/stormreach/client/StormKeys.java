package com.stormreach.client;

import com.stormreach.Stormreach;
import net.minecraft.client.KeyMapping;

/** The shared "Stormreach" section of the Controls screen (a category id can only be registered once). */
public final class StormKeys {
    private static KeyMapping.Category category;

    private StormKeys() {}

    public static synchronized KeyMapping.Category category() {
        if (category == null) category = KeyMapping.Category.register(Stormreach.id("stormreach"));
        return category;
    }
}
