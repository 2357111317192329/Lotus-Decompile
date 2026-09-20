/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnchantmentListSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.Utils
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EquipmentSlot
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.Enchantments
 *  net.minecraft.entity.mob.PiglinEntity
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.registry.entry.RegistryEntry
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoHelmet
extends BaseModule {
    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("检测距离")
        .description("检测猪灵的距离。")
        .defaultValue(10)
        .min(1)
        .sliderMax(40)
        .build());
    private final Setting<Integer> minTime = sgGeneral.add(new IntSetting.Builder()
        .name("检测时间-tick")
        .description("检测持续多少时间后开始切换头盔")
        .defaultValue(5)
        .min(1)
        .sliderMax(64)
        .build());
    private final Setting<Boolean> ignoreEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("忽略空")
        .description("没有穿戴头盔时, 不做切换")
        .defaultValue(true)
        .build());
    private final Setting<Set<ResourceKey<Enchantment>>> avoidedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("避免的附魔")
        .description("应该避免的附魔.")
        .defaultValue(new ResourceKey[]{Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER})
        .build());
    private final Object2IntMap<Holder<Enchantment>> enchantments;
    private long lastPiglinTime;
    private boolean timing;

    public AutoHelmet() {
        super("L自动头盔", "附近有猪灵时自动换上金头盔", 20);
        this.enchantments = new Object2IntOpenHashMap();
    }

    public void onActivate() {
        this.timing = false;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!this.isReady()) {
            return;
        }
        if (!this.checkAndDecrement()) {
            return;
        }
        ItemStack currentItemStack = this.mc.player.getItemBySlot(EquipmentSlot.HEAD);
        Utils.getEnchantments(currentItemStack, this.enchantments);
        if (this.enchantments.containsKey((Object)Enchantments.BINDING_CURSE)) {
            return;
        }
        boolean needGold = this.needGold();
        Item currentItem = currentItemStack.getItem();
        if (needGold) {
            if (currentItem == Items.GOLDEN_HELMET) {
                return;
            }
            if (!this.timing) {
                this.timing = true;
                this.lastPiglinTime = this.mcTime();
                return;
            }
            ItemStack nextStack = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == Items.GOLDEN_HELMET && !this.hasAvoidedEnchantment());
            if (nextStack.isEmpty()) {
                nextStack = this.nextPlayerStack((ItemStack itemStack) -> (itemStack.getItem() == Items.DIAMOND_HELMET || itemStack.getItem() == Items.NETHERITE_HELMET) && !this.hasAvoidedEnchantment());
            }
            if (!nextStack.isEmpty() && currentItem != nextStack.getItem() && this.lastPiglinTime + (long)this.minTime.get() < this.mcTime()) {
                if (this.ignoreEmpty.get() && currentItem == Items.AIR) {
                    return;
                }
                this.swap(this.getCurPlayerSlot(), 3);
                this.setDelay();
            }
        } else {
            this.timing = false;
            if (currentItem == Items.DIAMOND_HELMET || currentItem == Items.NETHERITE_HELMET) {
                return;
            }
            ItemStack nextStack = this.nextPlayerStack((ItemStack itemStack) -> (itemStack.getItem() == Items.DIAMOND_HELMET || itemStack.getItem() == Items.NETHERITE_HELMET) && !this.hasAvoidedEnchantment());
            if (!nextStack.isEmpty() && currentItem != nextStack.getItem()) {
                if (this.ignoreEmpty.get() && currentItem == Items.AIR) {
                    return;
                }
                this.swap(this.getCurPlayerSlot(), 3);
                this.setDelay();
            }
        }
    }

    private boolean needGold() {
        boolean hasPiglin = false;
        for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (entity == this.mc.player) continue;
            if (entity instanceof Player) {
                if (!AutoHelmet.isWithinDistance(entity, (Entity)this.mc.player, this.distance.get())) continue;
                return false;
            }
            if (!(entity instanceof Piglin) || !AutoHelmet.isWithinDistance(entity, (Entity)this.mc.player, this.distance.get())) continue;
            hasPiglin = true;
        }
        return hasPiglin;
    }

    public static boolean isWithinDistance(Entity a, Entity b, double distance) {
        return a.distanceToSqr(b) <= distance * distance;
    }

    private boolean hasAvoidedEnchantment() {
        for (Holder enchantment : this.enchantments.keySet()) {
            if (!enchantment.is(this.avoidedEnchantments.get()::contains)) continue;
            return true;
        }
        return false;
    }

    private void swap(int from, int armorSlotId) {
        InvUtils.move().from(from).toArmor(armorSlotId);
        this.info("切换盔甲", new Object[0]);
    }
}

