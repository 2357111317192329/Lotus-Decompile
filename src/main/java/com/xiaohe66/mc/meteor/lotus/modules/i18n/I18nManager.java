/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.Settings
 *  meteordevelopment.meteorclient.systems.hud.Hud
 *  meteordevelopment.meteorclient.systems.hud.HudElement
 *  meteordevelopment.meteorclient.systems.hud.HudElementInfo
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules.i18n;

import com.xiaohe66.mc.meteor.lotus.modules.I18nModule;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.MeteorModUtils;

import com.xiaohe66.mc.meteor.lotus.mixin.ModuleAccessor;
import com.xiaohe66.mc.meteor.lotus.mixin.SettingAccessor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18nManager {
    private static final Logger log = LoggerFactory.getLogger(I18nManager.class);
    public static final I18nManager INSTANCE = new I18nManager();
    private static final String ORIGIN_FILE_NAME = "Lotus-i18n-origin.properties";
    private static final String LANG_FILE_PREFIX = "Lotus-i18n-";
    private static final String MISSING_FILE_NAME = "Lotus-i18n-missing.properties";
    private static final String MISSING_LOTUS_FILE_NAME = "Lotus-i18n-missing_lotus.properties";
    private final Properties originTexts = new Properties();
    private final Properties translations = new Properties();
    private final List<ModuleTranslationEntry> moduleTranslationEntries = new ArrayList<ModuleTranslationEntry>();
    private final Map<Setting<?>, SettingTranslationEntry> settingTranslationEntries = new HashMap();
    private String currentLanguage;

    private I18nManager() {
        this.ensureDirectory();
    }

    public void init() {
        I18nModule i18nModule = (I18nModule)Modules.get().get(I18nModule.class);
        this.currentLanguage = (String)i18nModule.language.get();
        this.applyLanguage(this.currentLanguage);
    }

    public void register(HudElement hudElement) {
        HudElementInfo hudElementInfo = hudElement.info;
        if (hudElementInfo.group == Hud.GROUP) {
            String hudId = "hud-" + hudElementInfo.name;
            INSTANCE.register(hudId, hudElement.settings);
        }
    }

    public void register(Module module) {
        if (StringUtils.isBlank(module.name)) {
            return;
        }
        String modId = MeteorModUtils.getModId(module.addon);
        String titleKey = this.buildKey(modId, module.name);
        String descriptionKey = this.buildDescriptionKey(modId, module.name);
        String title = module.title == null ? "" : module.title;
        String description = module.description == null ? "" : module.description;
        this.originTexts.setProperty(titleKey, title);
        this.originTexts.setProperty(descriptionKey, description);
        this.moduleTranslationEntries.add(new ModuleTranslationEntry(module, title, description, titleKey, descriptionKey));
        this.applyModuleTranslation(module, titleKey, descriptionKey, title, description);
        this.register(titleKey, module.settings);
    }

    public void register(String prefix, Settings settings) {
        for (SettingGroup settingGroup : settings) {
            for (Setting setting : settingGroup) {
                this.register(prefix, setting);
            }
        }
    }

    public void register(String prefix, Setting<?> setting) {
        if (StringUtils.isBlank(setting.name)) {
            return;
        }
        String titleKey = this.buildKey(prefix, setting.name);
        String descriptionKey = this.buildDescriptionKey(prefix, setting.name);
        String title = setting.title == null ? "" : setting.title;
        String description = setting.description == null ? "" : setting.description;
        this.originTexts.setProperty(titleKey, title);
        this.originTexts.setProperty(descriptionKey, description);
        this.settingTranslationEntries.put(setting, new SettingTranslationEntry(setting, title, description, titleKey, descriptionKey));
        this.applySettingTranslation(setting, titleKey, descriptionKey, title, description);
    }

    public void unregister(Settings settings) {
        for (SettingGroup settingGroup : settings) {
            for (Setting setting : settingGroup) {
                this.settingTranslationEntries.remove(setting);
            }
        }
    }

    private void applyModuleTranslation(Module module, String titleKey, String descriptionKey, String title, String description) {
        String translatedTitle = this.translate(titleKey, title);
        String translatedDescription = this.translate(descriptionKey, description);
        ((ModuleAccessor)module).setTitle(translatedTitle);
        ((ModuleAccessor)module).setDescription(translatedDescription);
    }

    private void applySettingTranslation(Setting<?> setting, String titleKey, String descriptionKey, String title, String description) {
        String translatedTitle = this.translate(titleKey, title);
        String translatedDescription = this.translate(descriptionKey, description);
        ((SettingAccessor)setting).setTitle(translatedTitle);
        ((SettingAccessor)setting).setDescription(translatedDescription);
    }

    private String translate(String key, String defaultValue) {
        if ("origin".equals(this.currentLanguage)) {
            return defaultValue;
        }
        return this.translations.getProperty(key, defaultValue);
    }

    public void applyLanguage(String language) {
        this.loadTranslations(language);
        this.applyTranslations(language);
    }

    public void applyTranslations(String language) {
        this.currentLanguage = language;
        for (ModuleTranslationEntry entry : this.moduleTranslationEntries) {
            this.applyModuleTranslation(entry.module, entry.titleKey, entry.descriptionKey, entry.title, entry.description);
        }
        for (SettingTranslationEntry entry : this.settingTranslationEntries.values()) {
            this.applySettingTranslation(entry.setting, entry.titleKey, entry.descriptionKey, entry.title, entry.description);
        }
        ChatUtils.info((String)("重载[" + language + "]结束, 请关闭界面重新打开"), (Object[])new Object[0]);
        this.exportMissingFiles(language);
    }

    private void loadTranslations(String language) {
        this.translations.clear();
        if ("origin".equals(language)) {
            this.translations.putAll((Map<?, ?>)this.originTexts);
            return;
        }
        int loadedCount = 0;
        try (Stream<Path> paths = Files.list(this.getI18nDir());){
            List<Path> files = paths.filter(path -> {
                String fileName = path.getFileName().toString();
                return !ORIGIN_FILE_NAME.equals(fileName) && !MISSING_FILE_NAME.equals(fileName) && fileName.startsWith("Lotus-i18n-") && fileName.endsWith("-" + language + ".properties");
            }).sorted().toList();
            for (Path path : files) {
                try {
                    BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                    try {
                        Properties props = new Properties();
                        props.load(reader);
                        this.translations.putAll((Map<?, ?>)props);
                        ++loadedCount;
                        log.info("Loaded translations from {}", path.getFileName());
                    }
                    finally {
                        if (reader == null) continue;
                        reader.close();
                    }
                }
                catch (IOException e) {
                    log.error("Failed to load language file: {}", path, e);
                }
            }
        }
        catch (IOException e) {
            log.error("Failed to scan language files", (Throwable)e);
        }
        if (loadedCount == 0) {
            ChatUtils.warning((String)("语言文件不存在,[" + language + "]"), (Object[])new Object[0]);
        }
    }

    public void exportOriginFile() {
        Path file = this.getI18nDir().resolve(ORIGIN_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.originTexts.store(writer, "Original texts");
            log.info("Exported origin file: {}", file.getFileName());
        }
        catch (IOException e) {
            log.error("Failed to export origin file: {}", file, e);
        }
        this.exportMissingFiles(this.currentLanguage);
    }

    public void exportMissingFiles(String language) {
        if ("origin".equals(language)) {
            return;
        }
        Properties missingProps = new Properties();
        Properties missingLotusProps = new Properties();
        for (String key : this.originTexts.stringPropertyNames()) {
            if (this.translations.containsKey(key)) continue;
            if (key.startsWith("lotus")) {
                missingLotusProps.setProperty(key, this.originTexts.getProperty(key));
                continue;
            }
            missingProps.setProperty(key, this.originTexts.getProperty(key));
        }
        Path file = this.getI18nDir().resolve(MISSING_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, new OpenOption[0]);){
            missingProps.store(writer, "Missing translations for " + language);
            log.info("Exported missing file: {}, count: {}", file.getFileName(), missingProps.size());
        }
        catch (IOException e) {
            log.error("Failed to export missing file: {}", file, e);
        }
        Path lotusFile = this.getI18nDir().resolve(MISSING_LOTUS_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(lotusFile, StandardCharsets.UTF_8, new OpenOption[0]);){
            missingLotusProps.store(writer, "Missing Lotus translations for " + language);
            log.info("Exported missing Lotus file: {}, count: {}", lotusFile.getFileName(), missingLotusProps.size());
        }
        catch (IOException e) {
            log.error("Failed to export missing Lotus file: {}", lotusFile, e);
        }
    }

    public String[] getLanguages() {
        ArrayList<String> languages = new ArrayList<String>();
        try (Stream<Path> paths = Files.list(this.getI18nDir());){
            paths.forEach(path -> {
                String language;
                String modAndLang;
                String baseName;
                int dashIndex;
                String fileName = path.getFileName().toString();
                if (fileName.startsWith("Lotus-i18n-") && fileName.endsWith(".properties") && !fileName.equals(ORIGIN_FILE_NAME) && (dashIndex = (modAndLang = (baseName = fileName.substring("Lotus-i18n-".length())).substring(0, baseName.length() - 11)).lastIndexOf(45)) > 0 && !languages.contains(language = modAndLang.substring(dashIndex + 1))) {
                    languages.add(language);
                }
            });
        }
        catch (IOException e) {
            log.error("Failed to scan language files", (Throwable)e);
        }
        if (!languages.contains("origin")) {
            languages.add("origin");
        }
        return languages.toArray(new String[0]);
    }

    public String buildKey(String prefix, String name) {
        return prefix + "_" + name;
    }

    public String buildDescriptionKey(String prefix, String name) {
        return prefix + "_" + name + "_d";
    }

    public Path getI18nDir() {
        return Const.LOTUS_DIR.resolve("i18n");
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(this.getI18nDir(), new FileAttribute[0]);
        }
        catch (IOException e) {
            log.error("Failed to create Lotus directory", (Throwable)e);
        }
    }

    private record ModuleTranslationEntry(Module module, String title, String description, String titleKey, String descriptionKey) {
    }

    private record SettingTranslationEntry(Setting<?> setting, String title, String description, String titleKey, String descriptionKey) {
    }
}
