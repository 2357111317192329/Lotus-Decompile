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

import com.xiaohe66.mc.meteor.lotus.modules.ActivatedSpawnerDetector;
import com.xiaohe66.mc.meteor.lotus.modules.AutoCrafting;
import com.xiaohe66.mc.meteor.lotus.modules.AutoEnchantment;
import com.xiaohe66.mc.meteor.lotus.modules.AutoHelmet;
import com.xiaohe66.mc.meteor.lotus.modules.AutoMapCopy;
import com.xiaohe66.mc.meteor.lotus.modules.AutoPlaceMap;
import com.xiaohe66.mc.meteor.lotus.modules.AutoSignPlus;
import com.xiaohe66.mc.meteor.lotus.modules.EntityList;
import com.xiaohe66.mc.meteor.lotus.modules.GatherOrder;
import com.xiaohe66.mc.meteor.lotus.modules.I18nModule;
import com.xiaohe66.mc.meteor.lotus.modules.ItemClearUp;
import com.xiaohe66.mc.meteor.lotus.modules.i18n.I18nManager;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.util.ModsCheck;
import com.xiaohe66.mc.meteor.lotus.modules.LotusHelper;
import com.xiaohe66.mc.meteor.lotus.modules.MosquitoCoilScan;
import com.xiaohe66.mc.meteor.lotus.modules.PreviewTool;
import com.xiaohe66.mc.meteor.lotus.modules.RaidHelper;
import com.xiaohe66.mc.meteor.lotus.modules.RedstoneAssist;
import com.xiaohe66.mc.meteor.lotus.modules.StorageEspPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
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
        modules.add(new ActivatedSpawnerDetector());
        modules.add(new AutoCrafting());
        modules.add(new AutoEnchantment());
        modules.add(new AutoHelmet());
        modules.add(new AutoMapCopy());
        modules.add(new AutoPlaceMap());
        modules.add(new AutoSignPlus());
        modules.add(new EntityList());
        modules.add(new GatherOrder());
        modules.add(new RedstoneAssist());
        modules.add(new I18nModule());
        modules.add(new ItemClearUp());
        modules.add(new LotusHelper());
        modules.add(new MosquitoCoilScan());
        modules.add(new PreviewTool());
        modules.add(new RaidHelper());
        modules.add(new StorageEspPlus());
        if (ModsCheck.hasBaritone()) {
            BaritoneModules.add(modules);
        }
        if (ModsCheck.hasLitematica()) {
            SchematicModules.add(modules);
        }
        if (ModsCheck.hasBaritone() && ModsCheck.hasLitematica()) {
            BaritoneSchematicModules.add(modules);
        }
        MeteorClient.EVENT_BUS.subscribe(this);
        MeteorClient.EVENT_BUS.subscribe(new DelayedI18nRefreshTask(this));
    }

    @EventHandler
    private void onGameJoinedEvent(GameJoinedEvent event) {
        ChatUtils.warning("已加载【免费】彗星插件Lotus, xiaohe66出品", (Object[])new Object[0]);
        if (!ModsCheck.hasBaritone()) {
            ChatUtils.warning("缺少男中音", (Object[])new Object[0]);
        }
        Modules modules = Modules.get();
        GatherOrder gatherOrder = (GatherOrder)modules.get(GatherOrder.class);
        if (gatherOrder != null && gatherOrder.isPermanentlyDisabled() && !gatherOrder.isActive()) {
            gatherOrder.toggle();
        }
        MosquitoCoilScan mosquitoCoilScan = (MosquitoCoilScan)modules.get(MosquitoCoilScan.class);
        if (mosquitoCoilScan != null) {
            mosquitoCoilScan.start();
        }
        LotusHelper lotusHelper = (LotusHelper)modules.get(LotusHelper.class);
        if (lotusHelper != null) {
            LotusUtils.setServerId(lotusHelper.serverIdMode.get() == LotusHelper.ServerIdMode.ADDRESS);
        }
        PreviewTool previewTool = (PreviewTool)modules.get(PreviewTool.class);
        if (previewTool != null) {
            previewTool.refresh();
        }
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        MosquitoCoilScan mosquitoCoilScan = (MosquitoCoilScan)Modules.get().get(MosquitoCoilScan.class);
        if (mosquitoCoilScan != null) {
            mosquitoCoilScan.stop();
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

