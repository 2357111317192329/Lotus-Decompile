package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.AutoClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.AutoCrafting;
import com.xiaohe66.mc.meteor.lotus.modules.AutoEnchantment;
import com.xiaohe66.mc.meteor.lotus.modules.AutoHelmet;
import com.xiaohe66.mc.meteor.lotus.modules.AutoKit;
import com.xiaohe66.mc.meteor.lotus.modules.AutoPlaceMap;
import com.xiaohe66.mc.meteor.lotus.modules.AutoSignPlus;
import com.xiaohe66.mc.meteor.lotus.modules.ElytraFlyPlus;
import com.xiaohe66.mc.meteor.lotus.modules.EntityList;
import com.xiaohe66.mc.meteor.lotus.modules.GatherOrder;
import com.xiaohe66.mc.meteor.lotus.modules.ItemClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.LitematicaPrinter;
import com.xiaohe66.mc.meteor.lotus.modules.PacketMinePlus;
import com.xiaohe66.mc.meteor.lotus.modules.PreviewTool;
import com.xiaohe66.mc.meteor.lotus.modules.RaidHelper;
import com.xiaohe66.mc.meteor.lotus.modules.RedstoneAssist;
import com.xiaohe66.mc.meteor.lotus.modules.SafeWalkPlus;
import com.xiaohe66.mc.meteor.lotus.modules.StorageEspPlus;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerBookRoller;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerTrader;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.ModsCheck;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteorLotus extends MeteorAddon {
   private static final Logger log = LoggerFactory.getLogger(MeteorLotus.class);

   public void onInitialize() {
      log.info("Initializing xiaohe66-meteor-lotus");
      Modules modules = Modules.get();
      modules.add(new AutoCrafting());
      modules.add(new AutoEnchantment());
      modules.add(new AutoHelmet());
      modules.add(new AutoPlaceMap());
      modules.add(new AutoSignPlus());
      modules.add(new ElytraFlyPlus());
      modules.add(new EntityList());
      modules.add(new GatherOrder());
      modules.add(new RedstoneAssist());
      modules.add(new ItemClearUp());
      modules.add(new PacketMinePlus());
      modules.add(new PreviewTool());
      modules.add(new RaidHelper());
      modules.add(new SafeWalkPlus());
      modules.add(new StorageEspPlus());
      if (ModsCheck.hasWorldSchematic()) {
         modules.add(new LitematicaPrinter());
      }

      if (ModsCheck.hasBaritone()) {
         modules.add(new AutoClearUp());
         modules.add(new AutoKit());
         modules.add(new VillagerTrader());
         modules.add(new VillagerBookRoller());
      }

      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @EventHandler
   private void onGameJoinedEvent(GameJoinedEvent event) {
      ChatUtils.warning("已加载 xiaohe66-meteor-lotus, 此插件免费", new Object[0]);
      if (!ModsCheck.hasBaritone()) {
         ChatUtils.warning("缺少男中音", new Object[0]);
      }

      GatherOrder gatherOrder = (GatherOrder)Modules.get().get(GatherOrder.class);
      if (gatherOrder != null && gatherOrder.isPermanentlyDisabled() && !gatherOrder.isActive()) {
         gatherOrder.toggle();
      }
   }

   private void tryAddModule(String className, String msg, Supplier<Module> module) {
      Modules modules = Modules.get();
      log.info("try load : {}", className);

      try {
         Class.forName(className);
         log.info("try load success : {}", className);
         modules.add(module.get());
      } catch (ClassNotFoundException ignore) {
         log.warn(msg);
      }
   }

   public void onRegisterCategories() {
      Modules.registerCategory(Const.CATEGORY);
   }

   public String getPackage() {
      return "com.xiaohe66.mc.meteor.lotus";
   }
}
