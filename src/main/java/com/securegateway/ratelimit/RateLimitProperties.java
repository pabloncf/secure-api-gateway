package com.securegateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private int freeRpm = 10;
    private int proRpm = 60;
    private int adminRpm = -1;

    public int getFreeRpm() { return freeRpm; }
    public void setFreeRpm(int freeRpm) { this.freeRpm = freeRpm; }

    public int getProRpm() { return proRpm; }
    public void setProRpm(int proRpm) { this.proRpm = proRpm; }

    public int getAdminRpm() { return adminRpm; }
    public void setAdminRpm(int adminRpm) { this.adminRpm = adminRpm; }
}
