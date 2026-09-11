/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.meteorclient.addons.MeteorAddon
 *  meteordevelopment.meteorclient.events.game.GameJoinedEvent
 *  meteordevelopment.meteorclient.events.game.GameLeftEvent
 *  meteordevelopment.meteorclient.systems.modules.Category
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  meteordevelopment.orbit.EventHandler
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.AutoClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.AutoCrafting;
import com.xiaohe66.mc.meteor.lotus.modules.AutoEnchantment;
import com.xiaohe66.mc.meteor.lotus.modules.AutoHelmet;
import com.xiaohe66.mc.meteor.lotus.modules.AutoKit;
import com.xiaohe66.mc.meteor.lotus.modules.AutoPlaceMap;
import com.xiaohe66.mc.meteor.lotus.modules.AutoPrinterMap;
import com.xiaohe66.mc.meteor.lotus.modules.AutoSignPlus;
import com.xiaohe66.mc.meteor.lotus.modules.EntityList;
import com.xiaohe66.mc.meteor.lotus.modules.GatherOrder;
import com.xiaohe66.mc.meteor.lotus.modules.I18nModule;
import com.xiaohe66.mc.meteor.lotus.modules.ItemClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.i18n.I18nManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.ModsCheck;
import com.xiaohe66.mc.meteor.lotus.modules.MosquitoCoilScan;
import com.xiaohe66.mc.meteor.lotus.modules.PreviewTool;
import com.xiaohe66.mc.meteor.lotus.modules.LitematicaPrinter;
import com.xiaohe66.mc.meteor.lotus.modules.RaidHelper;
import com.xiaohe66.mc.meteor.lotus.modules.RedstoneAssist;
import com.xiaohe66.mc.meteor.lotus.modules.StorageEspPlus;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerBookRoller;
import com.xiaohe66.mc.meteor.lotus.modules.VillagerTrader;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteorLotus extends MeteorAddon {
    private static final Logger log = LoggerFactory.getLogger(MeteorLotus.class);

    public void onInitialize() {
        log.info("Initializing Lotus");
        Modules modules = Modules.get();
        modules.add(new AutoClearUp());
        modules.add(new AutoCrafting());
        modules.add(new AutoEnchantment());
        modules.add(new AutoHelmet());
        modules.add(new AutoKit());
        modules.add(new AutoPlaceMap());
        modules.add(new AutoPrinterMap());
        modules.add(new AutoSignPlus());
        modules.add(new EntityList());
        modules.add(new GatherOrder());
        modules.add(new RedstoneAssist());
        modules.add(new I18nModule());
        modules.add(new ItemClearUp());
        modules.add(new MosquitoCoilScan());
        modules.add(new PreviewTool());
        modules.add(new LitematicaPrinter());
        modules.add(new RaidHelper());
        modules.add(new StorageEspPlus());
        modules.add(new VillagerTrader());
        modules.add(new VillagerBookRoller());
        MeteorClient.EVENT_BUS.subscribe(this);
        MeteorClient.EVENT_BUS.subscribe(new DelayedI18nRefreshTask(this));
    }

    @EventHandler
    private void onGameJoinedEvent(GameJoinedEvent event) {
        MosquitoCoilScan mosquitoCoilScan;
        Modules modules;
        GatherOrder gatherOrder;
        ChatUtils.warning("已加载【免费】彗星插件lotus, xiaohe66出品", (Object[])new Object[0]);
        if (!ModsCheck.hasBaritone()) {
            ChatUtils.warning("缺少男中音", (Object[])new Object[0]);
        }
        if ((gatherOrder = (GatherOrder)(modules = Modules.get()).get(GatherOrder.class)) != null && gatherOrder.isPermanentlyDisabled() && !gatherOrder.isActive()) {
            gatherOrder.toggle();
        }
        if ((mosquitoCoilScan = (MosquitoCoilScan)modules.get(MosquitoCoilScan.class)) != null) {
            mosquitoCoilScan.start();
        }
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        MosquitoCoilScan mosquitoCoilScan = (MosquitoCoilScan)Modules.get().get(MosquitoCoilScan.class);
        if (mosquitoCoilScan != null) {
            mosquitoCoilScan.stop();
        }
    }

    private void tryAddModule(String className, String msg, Supplier<Module> module) {
        Modules modules = Modules.get();
        log.info("try load : {}", className);
        try {
            Class.forName(className);
            log.info("try load success : {}", className);
            modules.add(module.get());
        }
        catch (ClassNotFoundException e) {
            log.warn(msg);
        }
    }

    public void onRegisterCategories() {
        Modules.registerCategory((Category)Const.CATEGORY);
    }

    public String getPackage() {
        return "com.xiaohe66.mc.meteor.lotus";
    }

    private class DelayedI18nRefreshTask {
        private int tickCount = 0;

        DelayedI18nRefreshTask(MeteorLotus meteorLotus) {
        }

        @EventHandler
        private void onTick(TickEvent.Post event) {
            if (++this.tickCount >= 200) {
                I18nManager.INSTANCE.exportOriginFile();
                MeteorClient.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}

