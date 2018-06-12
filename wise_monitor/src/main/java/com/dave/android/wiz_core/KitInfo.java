package com.dave.android.wiz_core;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class KitInfo {
    private final String identifier;
    private final String version;
    private final String buildType;

    public KitInfo(String identifier, String version, String buildType) {
        this.identifier = identifier;
        this.version = version;
        this.buildType = buildType;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getVersion() {
        return this.version;
    }

    public String getBuildType() {
        return this.buildType;
    }
}
