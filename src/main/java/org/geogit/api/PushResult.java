package org.geogit.api;

public class PushResult {

    private STATUS status;

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public static enum STATUS {
        OK_APPLIED {
            @Override
            public int value() {
                return 0;
            }
        },
        CONFLICT {
            @Override
            public int value() {
                return 1;
            }
        };

        public abstract int value();

        public static STATUS valueOf(final int value) {
            return STATUS.values()[value];
        }
    }
}
