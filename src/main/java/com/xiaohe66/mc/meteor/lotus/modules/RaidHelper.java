/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.utils.Utils
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.entity.effect.StatusEffectInstance
 *  net.minecraft.entity.effect.StatusEffects
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.registry.entry.RegistryEntry
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;

import java.util.Collection;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RaidHelper extends BaseModule {
    private final Setting<Integer> before = sgGeneral.add(new IntSetting.Builder()
        .name("喝药提前量")
        .description("在<袭击之兆>结束的多少tick前喝药")
        .defaultValue(30)
        .sliderRange(0, 32)
        .build());
    private final Setting<Integer> drinkFailureTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("失败检测时间(秒)")
        .description("当喝药时间超过该时间未完成时, 停止喝药并开启杀戮")
        .defaultValue(5)
        .sliderRange(0, 10)
        .build());
    private final Setting<Boolean> one = sgGeneral.add(new BoolSetting.Builder()
        .name("保留一瓶")
        .description("当药快喝完时, 至少保留一瓶")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> showDrinkTimer = sgGeneral.add(new BoolSetting.Builder()
        .name("喝药计时")
        .description("是否输出喝药计时")
        .defaultValue(true)
        .build());
    public boolean drinking;
    private long drinkStartTime;

    public RaidHelper() {
        super("A袭击助手", "挂机袭击塔使用。定时从背包里拿药、自动喝药后开启杀戮光环", 560, 800);
    }

    public void onActivate() {
        this.setDelay(0);
        if (!this.needDrink()) {
            this.stopDrinking();
            LotusUtils.enableKillAura();
            this.setDelay();
            return;
        }
        this.drinking = false;
    }

    @EventHandler(priority=-100)
    private void onTick(TickEvent.Pre event) {
        if (!this.checkAndDecrement()) {
            int seconds;
            int delayTimer;
            if (this.showDrinkTimer.get() && (delayTimer = this.getDelayTimer()) % 20 == 0 && (seconds = delayTimer / 20) % 5 == 0) {
                this.info("喝药计时 : " + seconds, new Object[0]);
            }
            return;
        }
        if (this.drinking) {
            long now = System.currentTimeMillis();
            if (this.needDrink() && now - this.drinkStartTime < (long)this.drinkFailureTimeout.get() * 1000L) {
                this.drink();
            } else {
                this.info("喝药结束", new Object[0]);
                this.stopDrinking();
                LotusUtils.enableKillAura();
                this.setDelay();
            }
        } else {
            if (!this.needDrink()) {
                return;
            }
            LotusUtils.disableKillAura();
            boolean hasBottle = this.ensureBottleInMainHand();
            if (!hasBottle) {
                return;
            }
            this.info("开始喝药", new Object[0]);
            this.drinkStartTime = System.currentTimeMillis();
            this.drink();
        }
    }

    private void drink() {
        int slot = this.getMainSlot();
        ItemStack stack = this.getItemStack(slot);
        if (!this.isOminousBottle(stack)) {
            this.warning("状态异常, 药不在主手上", new Object[0]);
            this.ensureBottleInMainHand();
            return;
        }
        this.setPressed(true);
        if (!this.mc.player.isUsingItem()) {
            Utils.rightClick();
        }
        this.drinking = true;
    }

    private boolean ensureBottleInMainHand() {
        return this.ensureItemInMainHand(this::isOminousBottle, "缺少<不详之瓶>");
    }

    private boolean isOminousBottle(ItemStack stack) {
        return stack.getItem() == Items.OMINOUS_BOTTLE && (this.one.get() == false || stack.getCount() > 1);
    }

    private void stopDrinking() {
        this.setPressed(false);
        this.drinking = false;
    }

    private void setPressed(boolean pressed) {
        this.mc.options.keyUse.setDown(pressed);
    }

    private boolean needDrink() {
        return !this.hasBadOmen() && !this.hasRaidOmen();
    }

    private boolean hasBadOmen() {
        Collection<MobEffectInstance> effects = this.mc.player.getActiveEffects();
        for (MobEffectInstance effect : effects) {
            Holder effectType = effect.getEffect();
            if (effectType != MobEffects.BAD_OMEN) continue;
            return true;
        }
        return false;
    }

    private boolean hasRaidOmen() {
        Collection<MobEffectInstance> effects = this.mc.player.getActiveEffects();
        for (MobEffectInstance effect : effects) {
            Holder effectType = effect.getEffect();
            if (effectType != MobEffects.RAID_OMEN) continue;
            return effect.getDuration() >= this.before.get();
        }
        return false;
    }

    public void onDeactivate() {
        this.stopDrinking();
        LotusUtils.disableKillAura();
    }
}
