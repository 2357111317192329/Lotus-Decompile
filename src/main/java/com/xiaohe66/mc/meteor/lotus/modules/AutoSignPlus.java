/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.game.OpenScreenEvent
 *  meteordevelopment.meteorclient.events.packets.PacketEvent$Send
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.util.DyeColor
 *  net.minecraft.text.Text
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.block.entity.SignBlockEntity
 *  net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.text.MutableText
 *  net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen
 *  net.minecraft.block.entity.SignText
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Queue;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSignPlus
extends BaseModule {
    private static final Logger log = LoggerFactory.getLogger(AutoSignPlus.class);
    private static final String PLAYER_NAME_KEY = "${name}";
    private static final String TIME_KEY_START = "$time{";
    private static final String TIME_KEY_END = "}";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
        .name("第1行")
        .defaultValue("${name}")
        .build());
    private final Setting<String> line2 = sgGeneral.add(new StringSetting.Builder()
        .name("第2行")
        .defaultValue("was here,到此一游")
        .build());
    private final Setting<String> line3 = sgGeneral.add(new StringSetting.Builder()
        .name("第3行")
        .defaultValue("lotus by xiaohe66")
        .build());
    private final Setting<String> line4 = sgGeneral.add(new StringSetting.Builder()
        .name("第4行")
        .defaultValue("$time{yyyy.MM.dd HH:mm}")
        .build());
    private final Setting<Boolean> onlyEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("只填写空白牌子")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> autoSubmit = sgGeneral.add(new BoolSetting.Builder()
        .name("自动提交")
        .description("自动提交告示牌文本，不需要手动点击确定")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> syncSettings = sgGeneral.add(new BoolSetting.Builder()
        .name("同步设置")
        .description("手动编辑告示牌时，同步更新行文本设置")
        .defaultValue(false)
        .build());
    private final Queue<UpdateSignC2SPacket> pendingPackets;

    public AutoSignPlus() {
        super("L自动签名", "自动写牌子。${name}:玩家ID; $time{yyyy.MM.dd HH:mm}:时间可自定义表达式", 10);
        this.pendingPackets = new ArrayDeque<UpdateSignC2SPacket>();
    }

    public void onDeactivate() {
        this.pendingPackets.clear();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent openScreenEvent) {
        if (!this.autoSubmit.get()) {
            return;
        }
        Screen screen = openScreenEvent.screen;
        if (!(screen instanceof AbstractSignEditScreen)) {
            return;
        }
        AbstractSignEditScreen signEditScreen = (AbstractSignEditScreen)screen;
        SignBlockEntity sign = ((AbstractSignEditScreenAccessor)signEditScreen).meteor$getSign();
        if (this.onlyEmpty.get()) {
            SignText frontText = sign.getFrontText();
            for (Text text : frontText.getMessages(false)) {
                if (!StringUtils.isNotBlank(text.getString())) continue;
                return;
            }
        }
        this.pendingPackets.add(new UpdateSignC2SPacket(sign.getPos(), true, this.format(this.line1.get()), this.format(this.line2.get()), this.format(this.line3.get()), this.format(this.line4.get())));
        openScreenEvent.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Post post) {
        if (this.mc.player == null || this.pendingPackets.isEmpty()) {
            return;
        }
        if (!this.checkAndDecrement()) {
            return;
        }
        this.mc.player.networkHandler.sendPacket((Packet)this.pendingPackets.poll());
        this.setDelay();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!this.syncSettings.get()) {
            return;
        }
        Packet packet = event.packet;
        if (packet instanceof UpdateSignC2SPacket) {
            UpdateSignC2SPacket updatePacket = (UpdateSignC2SPacket)packet;
            log.info("UpdateSignC2SPacket : {}", updatePacket);
            if (updatePacket.isFront()) {
                String[] lineArr = updatePacket.getText();
                this.updateLine(this.line1, lineArr, 0);
                this.updateLine(this.line2, lineArr, 1);
                this.updateLine(this.line3, lineArr, 2);
                this.updateLine(this.line4, lineArr, 3);
            }
        }
    }

    private void updateLine(Setting<String> lineSetting, String[] lineArr, int index) {
        String lineConfigText = lineSetting.get();
        String lineNewText = lineArr[index];
        if (!(lineConfigText.contains("${name}") || lineConfigText.startsWith("$time{") && lineConfigText.endsWith(TIME_KEY_END))) {
            lineSetting.set(lineNewText);
        }
    }

    @EventHandler
    private void onHeOpenScreenEvent(HeOpenScreenEvent event) {
        if (this.autoSubmit.get()) {
            return;
        }
        log.info("onHeOpenScreenEvent : {}", event.screen);
        Screen screen = event.screen;
        if (screen instanceof AbstractSignEditScreen) {
            AbstractSignEditScreen signEditScreen = (AbstractSignEditScreen)screen;
            this.handleOpenScreen(event, signEditScreen);
        }
    }

    private void handleOpenScreen(HeOpenScreenEvent event, AbstractSignEditScreen screen) {
        Text[] originMessageArr;
        SignBlockEntity sign = ((AbstractSignEditScreenAccessor)screen).meteor$getSign();
        boolean isAllBlank = true;
        SignText frontText = sign.getFrontText();
        for (Text text : originMessageArr = frontText.getMessages(false)) {
            String lineText = text.getString();
            if (!StringUtils.isNotBlank(lineText)) continue;
            isAllBlank = false;
        }
        if (this.onlyEmpty.get() && !isAllBlank) {
            return;
        }
        Text[] lines = new Text[]{this.formatText(this.line1.get()), this.formatText(this.line2.get()), this.formatText(this.line3.get()), this.formatText(this.line4.get())};
        SignText signText = new SignText(lines, lines, DyeColor.BLACK, false);
        event.setSignText(signText);
    }

    private MutableText formatText(String text) {
        return Text.literal(this.format(text));
    }

    private String format(String text) {
        if (text.startsWith("$time{") && text.endsWith(TIME_KEY_END)) {
            String formatText = text.substring(PLAYER_NAME_KEY.length(), text.length() - 1);
            try {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatText);
                return dateTimeFormatter.format(LocalDateTime.now());
            }
            catch (Exception exception) {
                this.warning("时间表达式错误", new Object[0]);
                return DATE_TIME_FORMATTER.format(LocalDateTime.now());
            }
        }
        return text.replace(PLAYER_NAME_KEY, this.mc.player.getName().getString());
    }
}

