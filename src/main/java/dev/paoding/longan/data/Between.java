package dev.paoding.longan.data;

public class Between<T> {
    private String field;
    private T start;
    private T end;

    public static <T> Between<T> of(String field) {
        Between<T> between = new Between<>();
        between.field = field;
        return between;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public T getStart() {
        return start;
    }

    public void setStart(T start) {
        this.start = start;
    }

    public T getEnd() {
        return end;
    }

    public void setEnd(T end) {
        this.end = end;
    }
}
