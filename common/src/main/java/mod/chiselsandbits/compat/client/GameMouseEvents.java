package mod.chiselsandbits.compat.client;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public interface GameMouseEvents {

    Event<WheelScroll> BEFORE_SCROLL = EventFactory.of((listeners) -> ((deltaX, deltaY) -> {
        boolean result = false;
        for (final WheelScroll listener : listeners) {
            result |= listener.wheelScroll(deltaX, deltaY);
        }
        return result;
    }));

    interface WheelScroll {

        boolean wheelScroll(double deltaX, double deltaY);
    }
}
