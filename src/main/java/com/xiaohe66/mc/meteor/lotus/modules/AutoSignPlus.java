package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSignPlus extends Module {
   private static final Logger log = LoggerFactory.getLogger(AutoSignPlus.class);
   private static final String PLAYER_NAME_KEY = "${name}";
   private static final String TIME_KEY_START = "$time{";
   private static final String TIME_KEY_END = "}";
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> line1 = this.sgGeneral.add(((Builder)((Builder)new Builder().name("第1行")).defaultValue("${name}")).build());
   private final Setting<String> line2 = this.sgGeneral.add(((Builder)((Builder)new Builder().name("第2行")).defaultValue("was here,到此一游")).build());
   private final Setting<String> line3 = this.sgGeneral.add(((Builder)((Builder)new Builder().name("第3行")).defaultValue("lotus by xiaohe66")).build());
   private final Setting<String> line4 = this.sgGeneral.add(((Builder)((Builder)new Builder().name("第4行")).defaultValue("$time{yyyy.MM.dd HH:mm}")).build());
   private final Setting<Boolean> onlyEmpty = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("只填写空白牌子"))
               .defaultValue(true))
            .build()
      );

   public AutoSignPlus() {
      super(Const.CATEGORY, "自动签名", "自动写牌子。${name}:玩家ID; $time{yyyy.MM.dd HH:mm}:时间可自定义表达式");
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (event.packet instanceof ServerboundSignUpdatePacket packet) {
         log.info("UpdateSignC2SPacket : {}", packet);
         if (packet.isFrontText()) {
            String[] lineArr = packet.getLines();
            this.updateLine(this.line1, lineArr, 0);
            this.updateLine(this.line2, lineArr, 1);
            this.updateLine(this.line3, lineArr, 2);
            this.updateLine(this.line4, lineArr, 3);
         }
      }
   }

   private void updateLine(Setting<String> lineSetting, String[] lineArr, int index) {
      String lineConfigText = (String)lineSetting.get();
      String lineNewText = lineArr[index];
      if (!lineConfigText.contains("${name}") && (!lineConfigText.startsWith("$time{") || !lineConfigText.endsWith("}"))) {
         lineSetting.set(lineNewText);
      }
   }

   @EventHandler
   private void onHeOpenScreenEvent(HeOpenScreenEvent event) {
      log.info("onHeOpenScreenEvent : {}", event.screen);
      if (event.screen instanceof AbstractSignEditScreen screen) {
         this.handleOpenScreen(event, screen);
      }
   }

   private void handleOpenScreen(HeOpenScreenEvent event, AbstractSignEditScreen screen) {
      SignBlockEntity sign = ((AbstractSignEditScreenAccessor)screen).meteor$getSign();
      boolean isAllBlank = true;
      SignText frontText = sign.getFrontText();
      Component[] originMessageArr = frontText.getMessages(false);

      for (Component text : originMessageArr) {
         String lineText = text.getString();
         if (StringUtils.isNotBlank(lineText)) {
            isAllBlank = false;
         }
      }

      if (!(Boolean)this.onlyEmpty.get() || isAllBlank) {
         Component[] lines = new Component[]{
            this.formatText((String)this.line1.get()),
            this.formatText((String)this.line2.get()),
            this.formatText((String)this.line3.get()),
            this.formatText((String)this.line4.get())
         };
         SignText signText = new SignText(lines, lines, DyeColor.BLACK, false);
         event.setSignText(signText);
      }
   }

   private MutableComponent formatText(String text) {
      return Component.literal(this.format(text));
   }

   private String format(String text) {
      if (text.startsWith("$time{") && text.endsWith("}")) {
         String formatText = text.substring("${name}".length(), text.length() - 1);

         try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatText);
            return dateTimeFormatter.format(LocalDateTime.now());
         } catch (Exception e) {
            this.warning("时间表达式错误", new Object[0]);
            return DATE_TIME_FORMATTER.format(LocalDateTime.now());
         }
      } else {
         return text.replace("${name}", this.mc.player.getName().getString());
      }
   }
}
