package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.LinkedHashSet;
import java.util.Set;

public class LotusHelper extends Module {
    public enum ServerIdMode {
        NAME,
        ADDRESS
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<ServerIdMode> serverIdMode;
    public final Setting<String> lockedSlots;
    private final Set<Integer> lockedSlotSet;

    public LotusHelper() {
        super(Const.CATEGORY, "Lotus助手", "Lotus插件的辅助功能");
        this.serverIdMode = sgGeneral.add(new EnumSetting.Builder<ServerIdMode>()
            .name("服务器标识方式")
            .description("保存数据时, 文件夹的命名方式")
            .defaultValue(ServerIdMode.NAME)
            .build());
        this.lockedSlots = sgGeneral.add(new StringSetting.Builder()
            .name("锁定物品栏")
            .description("锁定的物品栏, 仓库管理时不操作该栏位的物品")
            .onChanged(this::onLockedSlotsChanged)
            .defaultValue("0,1,2,8")
            .build());
        this.lockedSlotSet = new LinkedHashSet<>();
    }

    private void onLockedSlotsChanged(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        this.lockedSlotSet.clear();
        String[] parts = value.trim().split(",");
        for (String part : parts) {
            try {
                int slot = Integer.parseInt(part);
                if (slot >= 0 && slot < 36) {
                    this.lockedSlotSet.add(slot);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        this.warning("新的锁定位: {}", new Object[]{this.lockedSlotSet});
    }

    public Set<Integer> getLockedSlots() {
        return this.lockedSlotSet;
    }
}
