/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.EnchantmentListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TradeRuleListSettingScreen extends WindowScreen {
    private final Setting<List<TradeRule>> setting;
    private WTable table;

    public TradeRuleListSettingScreen(GuiTheme theme, Setting<List<TradeRule>> setting) {
        super(theme, "交易对设置");
        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        add(theme.label("型别: 买=花绿宝石换物品 | 卖=卖物品换绿宝石 | 附魔书=绿宝石+普通书换附魔书")).expandX();
        add(theme.label("价格上限: 买/附魔书=买一个所需绿宝石数 | 卖=换1个绿宝石所需物品数")).expandX();
        add(theme.label("附魔书交易需从取货点取一本普通书")).expandX();
        table = add(theme.table()).expandX().widget();
        initTable();
    }

    private void initTable() {
        table.clear();
        List<TradeRule> rules = setting.get();

        // header
        table.add(theme.label("型别")).minWidth(60);
        table.add(theme.label("物品/附魔")).expandCellX();
        table.add(theme.label("选择"));
        table.add(theme.label("价格上限")).minWidth(60);
        table.add(theme.label(""));
        table.row();

        for (int i = 0; i < rules.size(); i++) {
            int index = i;
            TradeRule rule = rules.get(i);

            // 型別下拉
            WDropdown<TradeRule.Type> typeDropdown = theme.dropdown(TradeRule.Type.values(), rule.getType());
            typeDropdown.action = () -> {
                rule.setType(typeDropdown.get());
                setting.onChanged();
                initTable();
            };
            table.add(typeDropdown).minWidth(60);

            // 物品/附魔顯示
            boolean isBook = rule.getType() == TradeRule.Type.ENCHANTED_BOOK;
            ItemStack displayStack = isBook ? DisplayItemUtils.toStack(Items.ENCHANTED_BOOK) : DisplayItemUtils.toStack(rule.getItem());
            String displayName = isBook ? (rule.getEnchantment() != null ? Names.get(rule.getEnchantment()) : "未选择") : (rule.getItem() != null ? Names.get(rule.getItem()) : "未选择");
            WItemWithLabel itemLabel = table.add(theme.itemWithLabel(displayStack, displayName)).expandCellX().widget();
            itemLabel.tooltip = displayName;

            // 選擇按鈕
            WButton select = table.add(theme.button("选择")).widget();
            select.action = () -> {
                if (isBook) {
                    openEnchantmentPicker(rule);
                } else {
                    openItemPicker(rule);
                }
            };

            // 價格輸入
            WIntEdit priceEdit = theme.intEdit(rule.getPriceLimit(), 1, 64, true);
            priceEdit.action = () -> {
                rule.setPriceLimit(priceEdit.get());
                setting.onChanged();
            };
            table.add(priceEdit).minWidth(60);

            // 刪除
            WMinus minus = table.add(theme.minus()).widget();
            minus.action = () -> {
                setting.get().remove(index);
                setting.onChanged();
                initTable();
            };

            table.row();
        }

        if (!rules.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("添加交易对")).expandX().widget();
        add.action = () -> {
            setting.get().add(new TradeRule(TradeRule.Type.BUY, Items.AIR, null, 1));
            setting.onChanged();
            initTable();
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            initTable();
        };
        reset.tooltip = "Reset";
    }

    private void openItemPicker(TradeRule rule) {
        ItemSetting temp = new ItemSetting.Builder()
            .defaultValue(rule.getItem())
            .onChanged(item -> {
                rule.setItem(item);
                setting.onChanged();
            })
            .build();
        ItemSettingScreen screen = new ItemSettingScreen(theme, temp);
        screen.onClosed(this::initTable);
        mc.setScreen(screen);
    }

    private void openEnchantmentPicker(TradeRule rule) {
        EnchantmentListSetting temp = new EnchantmentListSetting.Builder()
            .defaultValue(rule.getEnchantment() != null ? Set.of(rule.getEnchantment()) : Set.of())
            .onChanged(set -> {
                if (!set.isEmpty()) {
                    rule.setEnchantment(set.iterator().next());
                    setting.onChanged();
                }
            })
            .build();
        EnchantmentListSettingScreen screen = new EnchantmentListSettingScreen(theme, temp);
        screen.onClosed(this::initTable);
        mc.setScreen(screen);
    }
}
