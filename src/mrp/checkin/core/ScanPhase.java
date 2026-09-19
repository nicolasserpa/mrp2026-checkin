package mrp.checkin.core;

/** Máquina de estados do fluxo de leitura. Substitui os ints PHASE_* da ScanActivity. */
public enum ScanPhase {
    SCANNING(0),
    VERIFYING(1),
    CONFIRMED(2),
    ERROR(3);

    private final int code;

    ScanPhase(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean allowsDecode() {
        return this == SCANNING;
    }

    public static ScanPhase fromCode(int code) {
        for (ScanPhase p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        return SCANNING;
    }
}