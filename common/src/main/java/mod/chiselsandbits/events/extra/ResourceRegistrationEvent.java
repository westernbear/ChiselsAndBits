package mod.chiselsandbits.events.extra;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public interface ResourceRegistrationEvent {

    Event<ResourceRegistration> EVENT = EventFactory.of(listeners -> () -> {
        for (ResourceRegistration listener : listeners) {
            listener.handle();
        }
    });

    interface ResourceRegistration {
        void handle();
    }
}
