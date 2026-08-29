package mod.chiselsandbits.helpers;

public enum ReadyState {
    PENDING_PRE,
    PENDING_INIT,
    PENDING_POST,
    READY,

    TRIGGER_PRE,
    TRIGGER_INIT,
    TRIGGER_POST;

    public boolean isReady() {
        return this == READY;
    }

    public ReadyState updateState(ReadyState trigger) {
        switch (trigger) {
            case TRIGGER_INIT:
                if (this == PENDING_INIT) {
                    return PENDING_POST;
                }
                throw new RuntimeException("Triggered " + trigger + " but was " + this);
            case TRIGGER_POST:
                if (this == PENDING_POST) {
                    return READY;
                }
                throw new RuntimeException("Triggered " + trigger + " but was " + this);
            case TRIGGER_PRE:
                if (this == PENDING_PRE) {
                    return PENDING_INIT;
                }
                throw new RuntimeException("Triggered " + trigger + " but was " + this);
            default:
        }

        throw new RuntimeException("Invalid Trigger");
    }
}
