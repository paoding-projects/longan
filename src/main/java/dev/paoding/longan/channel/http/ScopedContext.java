package dev.paoding.longan.channel.http;

public class ScopedContext {
    private ScopedValue.Carrier carrier;

    public ScopedContext attach(ScopedValue.Carrier carrier) {
        this.carrier = carrier;
        return this;
    }

    public void run(Runnable task) {
        if (carrier != null) {
            carrier.run(task);
        } else {
            task.run();
        }
    }
}
