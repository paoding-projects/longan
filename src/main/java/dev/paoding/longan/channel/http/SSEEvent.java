package dev.paoding.longan.channel.http;

import java.nio.charset.StandardCharsets;

public class SSEEvent {
    private final String id;
    private final String event;
    private final String data;
    private final Integer retry;

    private SSEEvent(Builder b) {
        this.id    = b.id;
        this.event = b.event;
        this.data  = b.data;
        this.retry = b.retry;
    }

    public static SSEEvent of(String data) {
        return new Builder().data(data).build();
    }

    public static SSEEvent of(String event, String data) {
        return new Builder().event(event).data(data).build();
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        if (id != null && !id.isEmpty()) {
            sb.append("id: ").append(id).append('\n');
        }
        if (event != null && !event.isEmpty()) {
            sb.append("event: ").append(event).append('\n');
        }
        if (retry != null) {
            sb.append("retry: ").append(retry).append('\n');
        }
        if (data != null) {
            for (String line : data.split("\n", -1)) {
                sb.append("data: ").append(line).append('\n');
            }
        } else {
            sb.append("data: \n");
        }
        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static class Builder {
        private String id;
        private String event;
        private String data;
        private Integer retry;

        public Builder id(String id)       { this.id    = id;    return this; }
        public Builder event(String event) { this.event = event; return this; }
        public Builder data(String data)   { this.data  = data;  return this; }
        public Builder retry(int ms)       { this.retry = ms;    return this; }
        public SSEEvent build()            { return new SSEEvent(this); }
    }
}
