package com.xiaohe66.mc.meteor.lotus.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);
    private static final Gson GSON = new Gson();

    private ConfigUtils() {
    }

    public static Path resolveConfigPath(String path) {
        return Const.LOTUS_DIR.resolve(path);
    }

    public static Map<String, List<String>> readConfig(String path) {
        if (path != null && !path.isBlank()) {
            Path file = resolveConfigPath(path);
            if (!Files.exists(file)) {
                return null;
            }
            try {
                String content = Files.readString(file);
                JsonObject json = GSON.fromJson(content, JsonObject.class);
                if (json == null) {
                    return null;
                }
                LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
                for (String key : json.keySet()) {
                    ArrayList<String> values = new ArrayList<>();
                    JsonElement element = json.get(key);
                    if (element != null && element.isJsonArray()) {
                        for (JsonElement item : element.getAsJsonArray()) {
                            values.add(item.getAsString());
                        }
                    }
                    result.put(key, values);
                }
                return result;
            } catch (Exception e) {
                LOGGER.error("读取配置文件失败: {}", file, e);
                return null;
            }
        }
        return null;
    }

    public static Map<Item, ClearUpMapping> loadMappings(String path) {
        HashMap<Item, ClearUpMapping> result = new HashMap<>();
        Map<String, List<String>> config = readConfig(path);
        if (config == null) {
            ChatUtils.warning("散装物品配置加载失败, 请检查文件: " + resolveConfigPath(path));
            return result;
        }
        for (Map.Entry<String, List<String>> entry : config.entrySet()) {
            Item target = parseItem(entry.getKey());
            if (target == null) {
                ChatUtils.warning("无法识别的目标物品: " + entry.getKey());
            } else {
                HashSet<Item> related = new HashSet<>();
                for (String id : entry.getValue()) {
                    Item item = parseItem(id);
                    if (item == null) {
                        ChatUtils.warning("无法识别的关联物品: " + id);
                    } else {
                        related.add(item);
                    }
                }
                result.put(target, new ClearUpMapping(target, related, true));
            }
        }
        return result;
    }

    public static void saveMappings(String path, Map<Item, ClearUpMapping> mappings) {
        LinkedHashMap<String, List<String>> json = new LinkedHashMap<>();
        for (ClearUpMapping mapping : mappings.values()) {
            String target = Registries.ITEM.getId(mapping.getTargetItem()).getPath();
            List<String> related = mapping.getRelatedItems().stream().map(item -> Registries.ITEM.getId(item).toString()).toList();
            json.put(target, related);
        }
        String content = GSON.toJson(json);
        Path file = resolveConfigPath(path);
        Path parent = file.getParent();
        try {
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            Files.writeString(file, content, StandardOpenOption.TRUNCATE_EXISTING);
            ChatUtils.warning("写入成功 : %s", file);
        } catch (IOException e) {
            ChatUtils.error("写入失败 : %s", file);
        }
    }

    public static String[] listConfigFiles() {
        Path dir = Const.LOTUS_DIR.resolve("warehouse");
        if (!Files.isDirectory(dir)) {
            return new String[]{"none"};
        }
        ArrayList<String> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .filter(ConfigUtils::isValidConfig)
                .forEach(p -> files.add(p.getFileName().toString()));
        } catch (IOException e) {
            LOGGER.error("读取 warehouse 文件夹失败", e);
            return new String[0];
        }
        files.sort(String::compareTo);
        return files.toArray(new String[0]);
    }

    private static boolean isValidConfig(Path path) {
        try {
            String content = Files.readString(path);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            return json != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        Item item = Registries.ITEM.get(identifier);
        return item == Items.AIR ? null : item;
    }
}
