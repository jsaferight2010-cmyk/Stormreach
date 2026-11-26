package com.stormreach.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 6.0: when the client's connection changes (joining a server, opening another world, disconnecting), the client-side
 * state from the last session is dropped: the weather HUD reading, the locker camera, the hail field and any
 * cinematic. Nothing from one server can show up on the next.
 */
public final class SessionReset {
    private SessionReset() {}

    private static Object connection;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Object now = mc.getConnection();
            if (now == connection) return;
            connection = now;
            ClientWeather.resetSession();
            com.stormreach.client.labyrinth.LockerClient.resetSession();
            com.stormreach.client.weather.HailClient.resetSession();
            com.stormreach.client.story.CinematicCamera.resetSession();
        });
    }
}
