/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.event;

public class Event {
    private final Stage stage;
    private boolean cancel = false;

    public Event(Stage stage) {
        this.stage = stage;
    }

    public void cancel() {
        this.setCancelled(true);
    }

    public boolean isCancel() {
        return this.cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isCancelled() {
        return this.cancel;
    }

    public Stage getStage() {
        return this.stage;
    }

    public boolean isPost() {
        return this.stage == Stage.Post;
    }

    public boolean isPre() {
        return this.stage == Stage.Pre;
    }

    public enum Stage {
        Pre,
        Post;
    }
}
