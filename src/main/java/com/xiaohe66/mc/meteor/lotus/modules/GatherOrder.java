package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.TaskUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatherOrder extends Module {
    private static final Logger log = LoggerFactory.getLogger(GatherOrder.class);
    private static final String VERSION_PLACEHOLDER = "{version}";
    private static final String RANDOM_PLACEHOLDER = "{random}";
    private static final String DEFAULT_MESSAGE = "{random} Lotus-{version}";
    private static final String VERSION;
    private static final String[] DEFAULT_POEMS = new String[]{"粉身碎骨浑不怕，要留清白在人间。", "咬定青山不放松，立根原在破岩中。", "出淤泥而不染，濯清涟而不妖。", "不要人夸好颜色，只留清气满乾坤。", "清风两袖朝天去，不带江南一寸棉。", "宁可枝头抱香死，何曾吹落北风中。", "海内存知己，天涯若比邻。", "晚来天欲雪，能饮一杯无？", "莫愁前路无知己，天下谁人不识君。", "浮云游子意，落日故人情。", "投我以木瓜，报之以琼琚。", "投我以桃，报之以李。", "落地为兄弟，何必骨肉亲。", "一生大笑能几回，斗酒相逢须醉倒。", "相知无远近，万里尚为邻。", "人生自古谁无死，留取丹心照汗青。", "生当作人杰，死亦为鬼雄。", "自古驱民在信诚，一言为重百金轻。", "三杯吐然诺，五岳倒为轻。", "酒逢知己千杯少，话不投机半句多。", "长风破浪会有时，直挂云帆济沧海。", "同是天涯沦落人，相逢何必曾相识。", "少壮不努力，老大徒伤悲。", "莫等闲，白了少年头，空悲切！"};
    private static String[] POEMS = DEFAULT_POEMS;
    private static boolean poemsLoaded = false;
    private final Random random = new Random();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> intervalMinutes = sgGeneral.add(new IntSetting.Builder()
        .name("发送间隔")
        .description("触发后发送消息的冷却时间（分钟）")
        .min(1)
        .sliderMax(60)
        .defaultValue(5)
        .build());
    private final Setting<String> keyword = sgGeneral.add(new StringSetting.Builder()
        .name("监听关键字")
        .description("公屏消息包含此关键字时自动发送消息")
        .defaultValue("挂电集结")
        .build());
    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("发送消息")
        .description("触发后自动发送的内容，支持{random} 和 {version} 占位符")
        .defaultValue("{random} Lotus-{version}")
        .build());
    private final Setting<Boolean> showVersion = sgGeneral.add(new BoolSetting.Builder()
        .name("显示版本")
        .description("勾选后，将发送消息中的{version}替换为插件版本号")
        .defaultValue(true)
        .build());
    private final Setting<Integer> minDelaySeconds = sgGeneral.add(new IntSetting.Builder()
        .name("随机延迟下限(秒)")
        .description("随机发送延迟的下限，单位秒")
        .min(0)
        .sliderRange(0, 60)
        .defaultValue(0)
        .build());
    private final Setting<Integer> maxDelaySeconds = sgGeneral.add(new IntSetting.Builder()
        .name("随机延迟上限(秒)")
        .description("随机发送延迟的上限，单位秒")
        .min(0)
        .sliderRange(0, 60)
        .defaultValue(2)
        .build());
    private final Setting<Boolean> randomSuffix = sgGeneral.add(new BoolSetting.Builder()
        .name("随机数字后缀")
        .description("勾选后，在发送的消息末尾追加随机数字后缀")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("勾选后，收到关键字时在客户端显示调试信息(冷却判定结果和发送计划)")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> permanentlyDisabled = sgGeneral.add(new BoolSetting.Builder()
        .name("自动启动")
        .description("勾选后，集结令功能将自动开启")
        .defaultValue(true)
        .build());
    private long lastSendTime = 0L;
    private long lastDebugTime = 0L;
    private boolean processingMessage = false;

    public GatherOrder() {
        super(Const.CATEGORY, "L集结令", "监听公屏聊天，出现符合关键字时自动发送自定义消息");
    }

    public void onActivate() {
        this.lastSendTime = 0L;
    }

    @PostInit
    public static void init() {
        loadPoems();
    }

    private static synchronized void loadPoems() {
        try {
            Path file = Const.LOTUS_DIR.resolve("poems.txt");
            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                ArrayList<String> poems = new ArrayList<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        poems.add(trimmed);
                    }
                }
                POEMS = poems.isEmpty() ? DEFAULT_POEMS : poems.toArray(new String[0]);
            } else {
                Files.createDirectories(file.getParent());
                Files.write(file, Arrays.asList(DEFAULT_POEMS), StandardCharsets.UTF_8);
                POEMS = DEFAULT_POEMS;
            }
        } catch (Exception e) {
            log.error("Failed to load poems from txt", e);
            POEMS = DEFAULT_POEMS;
        }
        poemsLoaded = true;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent receiveMessageEvent) {
        // 可重入防護: Module.info() 會把訊息加進聊天紀錄並再次觸發 ReceiveMessageEvent,
        // 若不攔截會形成同步無限循環導致客戶端崩潰
        if (!this.isActive() || this.processingMessage) {
            return;
        }
        this.processingMessage = true;
        try {
            String receivedMessage = receiveMessageEvent.getMessage().getString();
            String keywordText = this.keyword.get();
            if (keywordText.isBlank()) {
                return;
            }
            if (receivedMessage.contains(keywordText)) {
                long cooldown = (long)this.intervalMinutes.get() * 60L * 1000L;
                long currentTime = System.currentTimeMillis();
                long remaining = cooldown - (currentTime - this.lastSendTime);
                if (remaining > 0) {
                    this.debugInfo("收到集结指令, 冷却中, 剩余[%s]秒", new Object[]{remaining / 1000L});
                    return;
                }
                String resolvedMessage = this.resolveMessage();
                if (resolvedMessage.isBlank()) {
                    return;
                }
                int delay = this.randomDelayMillis();
                this.debugInfo("收到集结指令, 冷却通过, 将在[%s]毫秒后发送[%s]", new Object[]{delay, resolvedMessage});
                TaskUtils.run(() -> ChatUtils.sendPlayerMsg(resolvedMessage), (long)delay, TimeUnit.MILLISECONDS);
                this.lastSendTime = currentTime;
            }
        } finally {
            this.processingMessage = false;
        }
    }

    private void debugInfo(String format, Object... args) {
        if (!this.debugMode.get()) {
            return;
        }
        // 限速: 每秒最多输出1条调试信息, 防止聊天刷屏时淹没客户端
        long now = System.currentTimeMillis();
        if (now - this.lastDebugTime < 1000L) {
            return;
        }
        this.lastDebugTime = now;
        // 不能在事件派发内同步显示: Module.info 会 addMessage 触发嵌套的 ReceiveMessageEvent(单例),
        // 会污染外层事件导致原消息被取代/重复。因此延迟到独立线程, 在事件派发完全结束后再显示,
        // 且直接使用原本的 info(格式, 参数) 路径, 保持与普通 info 完全一致的格式。
        TaskUtils.run(() -> this.info(format, args), 0L, TimeUnit.MILLISECONDS);
    }

    private int randomDelayMillis() {
        int minMs = this.minDelaySeconds.get() * 1000;
        int maxMs = this.maxDelaySeconds.get() * 1000;
        if (maxMs <= minMs) {
            return minMs;
        }
        return RandomUtils.insecure().randomInt(minMs, maxMs + 1);
    }

    private String resolveMessage() {
        String result = this.message.get();
        if (result.contains("{random}")) {
            if (!poemsLoaded) {
                loadPoems();
            }
            int index = this.random.nextInt(POEMS.length);
            result = result.replace("{random}", POEMS[index]);
        }
        if (result.contains("{version}")) {
            result = result.replace("{version}", this.showVersion.get() ? VERSION : "");
        }
        if (this.randomSuffix.get()) {
            result = result + "   " + System.currentTimeMillis() % 1000L;
        }
        return result;
    }

    public boolean isPermanentlyDisabled() {
        return this.permanentlyDisabled.get();
    }

    static {
        VERSION = FabricLoader.getInstance().getModContainer("lotus").map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
}
