package dev.paoding.longan.core;


public class DataSourceConfig {
    private String name;
    private String username;
    private String password;
    private String url;
    private int idleMin = 10;
    private int poolMax = 100;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getIdleMin() {
        return idleMin;
    }

    public void setIdleMin(int idleMin) {
        this.idleMin = idleMin;
    }

    public int getPoolMax() {
        return poolMax;
    }

    public void setPoolMax(int poolMax) {
        this.poolMax = poolMax;
    }
}
