package com.stormreach.sim;

/**
 * Backpack tiers in progression order. Each tier is crafted from the previous one. The three custom-ore tiers
 * come after Netherite because each also needs boss- or realm-tier mod materials (Tempest Core, realm
 * Voltite, Aurorite, and finally the Maelstrom Core from the last boss), which are harder to get than
 * netherite. Among themselves they follow the mod's own ore value: Storm Crystal (Overworld) &lt; Voltite
 * (realm, common) &lt; Aurorite (realm, rare).
 */
public enum BackpackTier {
    DIAMOND("diamond", 27), OBSIDIAN("obsidian", 36), NETHERITE("netherite", 54),
    STORM_CRYSTAL("storm_crystal", 72), VOLTITE("voltite", 90), AURORITE("aurorite", 108), MAELSTROM("maelstrom", 135);

    public static final int PAGE_SIZE = 54;
    public static final int CAPACITY_PER_LEVEL = 9;
    public static final int MAX_CAPACITY_LEVEL = 3;

    public final String id;
    public final int baseSlots;

    BackpackTier(String id, int baseSlots) {
        this.id = id;
        this.baseSlots = baseSlots;
    }

    /** Total slots with the Capacity enchantment at the given level. */
    public int slots(int capacityLevel) {
        int lvl = Math.max(0, Math.min(MAX_CAPACITY_LEVEL, capacityLevel));
        return baseSlots + lvl * CAPACITY_PER_LEVEL;
    }

    public BackpackTier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    public static int pages(int slots) {
        return Math.max(1, (slots + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** Rows shown in the menu: just enough for single-page packs, 6 for paged packs. */
    public static int rows(int slots) {
        return slots <= PAGE_SIZE ? Math.max(1, (slots + 8) / 9) : 6;
    }

    /** Backing index for a visible slot on a page, or -1 when that slot doesn't exist (last page). */
    public static int backingIndex(int slots, int page, int visibleSlot) {
        int idx = page * PAGE_SIZE + visibleSlot;
        return idx < slots ? idx : -1;
    }

    public static BackpackTier byId(String id) {
        for (BackpackTier t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
