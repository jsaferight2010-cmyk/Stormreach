package com.stormreach.client.anim;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state for the rigged Stormreach mobs: everything their keyframed animation reads. */
public class RigState extends LivingEntityRenderState {
    /** Ticks since the last attack or cast pulse (large if none) and which it was. */
    public float attackAge = 1e6f;
    public int attackKind;
    /** Ticks since it was last hurt (large if not hurting). */
    public float hurtAge = 1e6f;
    /** Smoothed 0..1: how airborne it is (0 on the ground). */
    public float air;
    /** Smoothed horizontal speed (blocks/tick), vertical speed, and body turn rate (degrees/tick). */
    public float speed, vy, turn;
    /** A per-entity constant in [0,1), so a herd doesn't move in lockstep. */
    public float seed;
    public boolean saddled, sheared, charged, anchored;
    /** 6.0: the local player is driving this vehicle in first person (draw the cockpit, not the cab). */
    public boolean firstPerson;
    /** A smoothed 0..1 value a renderer can drive (a hermit crab pulling into its shell, a vehicle's anchors). */
    public float hide;
}
