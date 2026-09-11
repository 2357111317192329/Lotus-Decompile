/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.gui.GuiTheme
 *  meteordevelopment.meteorclient.gui.GuiThemes
 *  meteordevelopment.meteorclient.gui.widgets.WWidget
 *  meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList
 *  meteordevelopment.meteorclient.gui.widgets.pressable.WButton
 *  meteordevelopment.meteorclient.pathing.PathManagers
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ProvidedStringSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  meteordevelopment.meteorclient.systems.config.Config
 *  meteordevelopment.meteorclient.systems.hud.Hud
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.utils.PostInit
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.i18n.I18nManager;
import com.xiaohe66.mc.meteor.lotus.util.Const;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class I18nModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<String> language = sgGeneral.add(new ProvidedStringSetting.Builder()
        .name("语言")
        .description("选择要加载的语言文件，修改后自动重载")
        .defaultValue("origin")
        .supplier(I18nManager.INSTANCE::getLanguages)
        .onChanged(I18nManager.INSTANCE::applyTranslations)
        .build());
    private final Setting<Boolean> disableCustomFont = sgGeneral.add(new BoolSetting.Builder()
        .name("取消自定义字体")
        .description("勾选后, 在启动游戏后自动取消自定义字体")
        .defaultValue(true)
        .build());

    public I18nModule() {
        super(Const.CATEGORY, "国际化", "Lotus国际化, 支持国际化其他插件。(更新语言后需要重新打开GUI)");
    }

    @PostInit
    public static void init() {
        I18nManager.INSTANCE.init();
        Modules modules = Modules.get();
        for (Module module : modules.getAll()) {
            I18nManager.INSTANCE.register(module);
        }
        I18nManager.INSTANCE.register("config", Config.get().settings);
        I18nManager.INSTANCE.register("gui", GuiThemes.get().settings);
        I18nManager.INSTANCE.register("hud", Hud.get().settings);
        Settings settings = PathManagers.get().getSettings().get();
        I18nManager.INSTANCE.register("baritone", settings);
        I18nModule module = (I18nModule)modules.get(I18nModule.class);
        if (module != null && module.disableCustomFont.get()) {
            Config.get().customFont.set(false);
        }
    }

    public WWidget getWidget(GuiTheme guiTheme) {
        WVerticalList list = guiTheme.verticalList();
        WButton reloadButton = (WButton)list.add((WWidget)guiTheme.button("重新加载配置")).expandX().widget();
        reloadButton.action = () -> I18nManager.INSTANCE.applyLanguage(this.language.get());
        WButton openFolderButton = (WButton)list.add((WWidget)guiTheme.button("打开配置文件夹")).expandX().widget();
        openFolderButton.action = () -> {
            try {
                this.openFolder(I18nManager.INSTANCE.getI18nDir().toFile());
            }
            catch (IOException e) {
                ChatUtils.error("打开 i18n 配置文件夹失败: " + e.getMessage(), (Object[])new Object[0]);
            }
        };
        return list;
    }

    private void openFolder(File folder) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", folder.getAbsolutePath()});
        } else if (osName.contains("mac")) {
            Runtime.getRuntime().exec(new String[]{"open", folder.getAbsolutePath()});
        } else {
            Runtime.getRuntime().exec(new String[]{"xdg-open", folder.getAbsolutePath()});
        }
    }

    public void onActivate() {
        this.toggle();
    }
}
