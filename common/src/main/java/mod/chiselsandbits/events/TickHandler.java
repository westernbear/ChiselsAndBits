package mod.chiselsandbits.events;

import dev.architectury.event.events.client.ClientTickEvent;

public final class TickHandler {

    private static long clientTicks = 0;

    private TickHandler() {}

    public static void register() {
        ClientTickEvent.CLIENT_PRE.register(instance -> clientTicks++);
    }

    public static long getClientTicks() {
        return clientTicks;
    }
}
