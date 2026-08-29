package mod.chiselsandbits.events.extra;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public interface EntityItemPickupEvent {

    Event<EntityItemPickup> EVENT = EventFactory.of(listeners -> (itemEntity, player) -> {
        boolean result = false;
        for (EntityItemPickup listener : listeners) {
            result |= listener.handle(itemEntity, player);
        }
        return result;
    });

    interface EntityItemPickup {
        boolean handle(ItemEntity itemEntity, Player player);
    }
}
