package dev.paoding.longan.channel.http;

public class ScopedResult {
    private ScopedValue.Carrier carrier;
    private boolean permitted;

    private ScopedResult() {
    }

    public static ScopedResult of(boolean permitted) {
        ScopedResult scopedResult = new ScopedResult();
        scopedResult.permitted = permitted;
        return scopedResult;
    }

    public ScopedResult bind(ScopedValue.Carrier carrier) {
        this.carrier = carrier;
        return this;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public void run(Runnable op) {
        if (carrier != null) {
            carrier.run(op);
        } else {
            op.run();
        }
    }
}
