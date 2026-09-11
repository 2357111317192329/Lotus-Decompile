/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;

public class ElytraTakeoffEvent extends Event {
    private final TakeoffResult result;
    private final String reason;

    public ElytraTakeoffEvent(TakeoffResult result) {
        this(result, null);
    }

    public ElytraTakeoffEvent(TakeoffResult result, String reason) {
        super(Stage.Post);
        this.result = result;
        this.reason = reason;
    }

    public TakeoffResult getResult() {
        return this.result;
    }

    public String getReason() {
        return this.reason;
    }

    public boolean isSuccess() {
        return this.result == TakeoffResult.SUCCESS;
    }

    public boolean isFailed() {
        return this.result == TakeoffResult.FAILED;
    }

    public enum TakeoffResult {
        SUCCESS,
        FAILED;
    }
}

