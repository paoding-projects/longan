package dev.paoding.longan.channel.http;


public class HttpCookie {
    private String name;
    private String value;
    private boolean wrap;
    private String domain;
    private String path;
    private boolean secure;
    private boolean httpOnly;
    private SameSite sameSite;
    private boolean partitioned;
    private long maxAge = Long.MIN_VALUE;

    public HttpCookie(String name,String value){
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }


    public boolean wrap() {
        return this.wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public String domain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long maxAge() {
        return this.maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;

    }

    public boolean isSecure() {
        return this.secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public SameSite sameSite() {
        return this.sameSite;
    }

    public void setSameSite(SameSite sameSite) {
        this.sameSite = sameSite;
    }

    public boolean isPartitioned() {
        return this.partitioned;
    }

    public void setPartitioned(boolean partitioned) {
        this.partitioned = partitioned;
    }

    public enum SameSite {
        Lax,
        Strict,
        None;

        static SameSite of(String name) {
            if (name != null) {
                for (SameSite each : (SameSite[]) SameSite.class.getEnumConstants()) {
                    if (each.name().equalsIgnoreCase(name)) {
                        return each;
                    }
                }
            }

            return null;
        }
    }
}