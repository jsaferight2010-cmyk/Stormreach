package com.stormreach.sim;

/** Caps how much work one tornado may do: block checks per tick and airborne debris alive at once. */
public final class DebrisBudget {
    private final int maxAirborne;
    private final int checksPerTick;
    private int airborne;
    private int checksThisTick;

    public DebrisBudget(int maxAirborne, int checksPerTick) {
        this.maxAirborne = Math.max(0, maxAirborne);
        this.checksPerTick = Math.max(0, checksPerTick);
    }

    public void newTick() {
        checksThisTick = 0;
    }

    public boolean tryCheck() {
        if (checksThisTick >= checksPerTick) return false;
        checksThisTick++;
        return true;
    }

    public boolean tryLaunch() {
        if (airborne >= maxAirborne) return false;
        airborne++;
        return true;
    }

    public void landed() {
        if (airborne > 0) airborne--;
    }

    public void setAirborne(int count) {
        airborne = Math.max(0, count);
    }

    public int airborne() {
        return airborne;
    }

    public int checksThisTick() {
        return checksThisTick;
    }
}
