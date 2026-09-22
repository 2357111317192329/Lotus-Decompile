/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.registry.RegistryKey
 */
package com.xiaohe66.mc.meteor.lotus.event;

import com.xiaohe66.mc.meteor.lotus.event.Event;
import java.util.Objects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class LogEvent extends Event {
    private String moduleName;
    private RegistryKey<World> worldType;
    private Vec3i pos;
    private String key;
    private String msg;
    private transient String formatKey;

    public LogEvent() {
        super(Stage.Post);
    }

    public String formatKey() {
        if (this.formatKey == null) {
            this.formatKey = this.moduleName + "_" + this.worldType.getValue().getPath() + "_" + this.key + "_";
        }
        return this.formatKey;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public RegistryKey<World> getWorldType() {
        return this.worldType;
    }

    public void setWorldType(RegistryKey<World> worldType) {
        this.worldType = worldType;
    }

    public Vec3i getPos() {
        return this.pos;
    }

    public void setPos(Vec3i pos) {
        this.pos = pos;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        LogEvent logEvent = (LogEvent)o;
        if (!Objects.equals(this.moduleName, logEvent.moduleName)) {
            return false;
        }
        if (!Objects.equals(this.pos, logEvent.pos)) {
            return false;
        }
        return Objects.equals(this.msg, logEvent.msg);
    }

    public int hashCode() {
        int result = this.moduleName != null ? this.moduleName.hashCode() : 0;
        result = 31 * result + (this.pos != null ? this.pos.hashCode() : 0);
        result = 31 * result + (this.msg != null ? this.msg.hashCode() : 0);
        return result;
    }
}
