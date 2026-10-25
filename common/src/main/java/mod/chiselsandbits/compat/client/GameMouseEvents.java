package mod.chiselsandbits.compat.client;

public interface GameMouseEvents {

    ClientEvent<WheelScroll> BEFORE_SCROLL =
            ClientEvent.arrayBacked(WheelScroll.class, (listeners) -> ((deltaX, deltaY) -> {
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
