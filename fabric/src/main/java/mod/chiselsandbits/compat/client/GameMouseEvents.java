package mod.chiselsandbits.compat.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface GameMouseEvents {

    Event<WheelScroll> BEFORE_SCROLL =
            EventFactory.createArrayBacked(WheelScroll.class, (listeners) -> ((deltaX, deltaY) -> {
                boolean result = false;
                for (WheelScroll listener : listeners) {
                    result |= listener.wheelScroll(deltaX, deltaY);
                }
                return result;
            }));

    interface WheelScroll {

        boolean wheelScroll(double deltaX, double deltaY);
    }
}
