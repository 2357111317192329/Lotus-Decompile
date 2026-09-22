/*
 * Port of Lotus 19.5 ap (PreviewTool module, decompiled bytecode-accurate) to
 * the 18.2 codebase (Mojang-mapped MC 26.1.2).
 *
 * Features:
 *  盒子图标   – shulker box slot icons (KitIconRenderer, 19.5 aQ)
 *  盒子预览   – pinned box/bundle preview tooltip (PinnedPreviewRenderer, 19.5 aJ)
 *  容器标识   – container mark icons + highlights (ContainerMarkRenderer aN / ContainerHighlightRenderer aM)
 *  盒子平铺   – kit spread in open screens (KitSpreadRenderer, 19.5 aR)
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.ContainerTooltipTextEvent;
import com.xiaohe66.mc.meteor.lotus.event.DrawMouseoverTooltipEvent;
import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.IsPointOverSlotEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

public class PreviewTool extends BaseModule {
    private final SettingGroup iconGroup = settings.createGroup("盒子图标");
    public final Setting<Boolean> kitIcon;
    private final Setting<Double> iconScale;
    private final Setting<Integer> iconOffsetX;
    private final Setting<Integer> iconOffsetY;
    private final SettingGroup previewGroup = settings.createGroup("盒子预览");
    public final Setting<Boolean> boxPreview;
    private final Setting<Keybind> pinKey;
    private final SettingGroup markGroup = settings.createGroup("容器标识");
    public final Setting<Boolean> containerMark;
    private final Setting<Double> markScale;
    private final Setting<Integer> markDistance;
    private final Setting<Boolean> highlightChests;
    private final Setting<Boolean> highlightFrames;
    private final Setting<Integer> highlightDistance;
    public final Setting<SettingColor> highlightColor;
    private final Setting<Boolean> persistCache;
    private final SettingGroup kitSpreadGroup = settings.createGroup("盒子平铺");
    public final Setting<Boolean> kitSpread;
    private final Setting<Integer> top;
    private final Setting<Integer> left;
    private final Setting<Integer> spacing;
    private final Setting<Double> scale;
    private final Setting<Boolean> compact;
    private final Setting<Integer> backgroundAlpha;

    private final KitIconRenderer kitIconRenderer;
    private final KitSpreadRenderer kitSpreadRenderer;
    private final PinnedPreviewRenderer previewRenderer;
    private final ContainerMarkRenderer markRenderer;
    private final ContainerHighlightRenderer highlightRenderer;

    public PreviewTool() {
        super("V预览器", "盒子小图标预览、盒子内容平铺、容器标识");
        this.kitIcon = this.iconGroup.add(new BoolSetting.Builder()
            .name("盒子图标")
            .description("显示盒子数量最多的物品(以组为单位)")
            .defaultValue(true)
            .build());
        this.iconScale = this.iconGroup.add(new DoubleSetting.Builder()
            .name("图标比例")
            .description("盒子图标比例")
            .defaultValue(0.4)
            .sliderRange(0.1, 1.0)
            .build());
        this.iconOffsetX = this.iconGroup.add(new IntSetting.Builder()
            .name("图标偏移量X")
            .description("偏移量")
            .defaultValue(7)
            .sliderRange(-20, 20)
            .build());
        this.iconOffsetY = this.iconGroup.add(new IntSetting.Builder()
            .name("图标偏移量Y")
            .description("偏移量")
            .defaultValue(7)
            .sliderRange(-20, 20)
            .build());
        this.boxPreview = this.previewGroup.add(new BoolSetting.Builder()
            .name("盒子预览")
            .description("鼠标悬停在盒子/收纳袋上时显示内容预览，按一下固定键可固定预览；固定后鼠标放在预览中的盒子/收纳袋上再按固定键，可再固定一层预览")
            .defaultValue(true)
            .build());
        this.pinKey = this.previewGroup.add(new KeybindSetting.Builder()
            .name("固定按键")
            .description("固定盒子预览的按键")
            .defaultValue(Keybind.fromKey(341))
            .build());
        this.containerMark = this.markGroup.add(new BoolSetting.Builder()
            .name("容器标识")
            .description("展示打开过的容器中占比最高的物品，在容器朝向玩家的那一面显示图标")
            .defaultValue(true)
            .build());
        this.markScale = this.markGroup.add(new DoubleSetting.Builder()
            .name("标识大小")
            .description("容器标识图标的显示大小")
            .defaultValue(1.0)
            .sliderRange(0.5, 2.0)
            .build());
        this.markDistance = this.markGroup.add(new IntSetting.Builder()
            .name("标识距离")
            .description("超过此距离不显示容器标识")
            .defaultValue(32)
            .min(4)
            .sliderMax(64)
            .build());
        this.highlightChests = this.markGroup.add(new BoolSetting.Builder()
            .name("高亮箱子")
            .description("高亮显示包含快捷栏选中物品的箱子")
            .defaultValue(true)
            .build());
        this.highlightFrames = this.markGroup.add(new BoolSetting.Builder()
            .name("高亮展示框")
            .description("高亮显示包含快捷栏选中物品的展示框")
            .defaultValue(true)
            .build());
        this.highlightDistance = this.markGroup.add(new IntSetting.Builder()
            .name("高亮距离")
            .description("超过此距离不显示高亮效果")
            .defaultValue(64)
            .min(4)
            .sliderMax(128)
            .build());
        this.highlightColor = this.sgGeneral.add(new ColorSetting.Builder()
            .name("高亮颜色")
            .defaultValue(new Color(255, 215, 0, 64))
            .build());
        this.persistCache = this.markGroup.add(new BoolSetting.Builder()
            .name("持久化缓存")
            .description("将容器标识缓存保存到硬盘（Lotus/container-mark 目录下），重启游戏后恢复")
            .defaultValue(false)
            .onChanged(this::persistCacheChanged)
            .build());
        this.kitSpread = this.kitSpreadGroup.add(new BoolSetting.Builder()
            .name("盒子平铺")
            .description("将kit在界面中平铺显示")
            .defaultValue(true)
            .build());
        this.top = this.kitSpreadGroup.add(new IntSetting.Builder()
            .name("上边距")
            .description("上边距")
            .defaultValue(6)
            .min(0)
            .sliderMax(100)
            .build());
        this.left = this.kitSpreadGroup.add(new IntSetting.Builder()
            .name("左边距")
            .description("左边距")
            .defaultValue(6)
            .min(0)
            .sliderMax(500)
            .build());
        this.spacing = this.kitSpreadGroup.add(new IntSetting.Builder()
            .name("间距")
            .description("间距")
            .defaultValue(4)
            .min(0)
            .sliderMax(10)
            .build());
        this.scale = this.kitSpreadGroup.add(new DoubleSetting.Builder()
            .name("缩放")
            .description("平铺区域的缩放比例")
            .defaultValue(1.0)
            .sliderRange(0.5, 1.5)
            .build());
        this.compact = this.kitSpreadGroup.add(new BoolSetting.Builder()
            .name("紧凑模式")
            .description("将相同物品合并显示，节省空间")
            .defaultValue(true)
            .build());
        this.backgroundAlpha = this.kitSpreadGroup.add(new IntSetting.Builder()
            .name("背景透明度")
            .description("盒子背景的不透明度")
            .defaultValue(30)
            .min(0)
            .sliderMax(255)
            .build());
        this.kitIconRenderer = new KitIconRenderer(this.iconScale, this.iconOffsetX, this.iconOffsetY);
        this.kitSpreadRenderer = new KitSpreadRenderer(this.top, this.left, this.spacing, this.scale, this.compact, this.backgroundAlpha);
        this.previewRenderer = new PinnedPreviewRenderer(this.boxPreview, this.pinKey);
        this.markRenderer = new ContainerMarkRenderer(this.markScale, this.markDistance, this.persistCache);
        this.highlightRenderer = new ContainerHighlightRenderer(this.markRenderer, this.highlightChests, this.highlightFrames, this.highlightDistance, this.highlightColor, this.markDistance);
    }

    /** Called by MeteorLotus on game join: refresh everything and load the persisted cache. */
    public void refresh() {
        this.refreshAll();
        this.markRenderer.loadCache();
    }

    @EventHandler
    private void onOpenScreen(ScreenRenderEvent event) {
        if (this.kitSpread.get() && this.mc.currentScreen instanceof HandledScreen) {
            this.kitSpreadRenderer.onOpenScreen(event);
        }
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (this.kitSpread.get() && this.mc.currentScreen instanceof HandledScreen) {
            this.kitSpreadRenderer.onMouseScroll(event);
        }
    }

    @EventHandler
    private void onHandledScreenRenderEvent(HandledScreenRenderEvent event) {
        Screen screen = this.mc.currentScreen;
        if (screen instanceof HandledScreen handledScreen) {
            if (this.boxPreview.get()) {
                this.previewRenderer.updatePinned(event.getMouseX(), event.getMouseY(), event.getHoveredSlot());
            }
            ScreenHandler menu = handledScreen.getScreenHandler();
            this.markRenderer.update(menu);
            if (this.kitIcon.get()) {
                this.kitIconRenderer.render(event.getDrawContext(), menu.slots);
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        this.markRenderer.clearTick();
        if (this.kitIcon.get() && !(this.mc.currentScreen instanceof HandledScreen)) {
            this.kitIconRenderer.render(event.drawContext);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        this.markRenderer.tick();
        if (this.containerMark.get() && this.isReady()) {
            this.highlightRenderer.render3D(event);
        }
    }

    @EventHandler
    private void onContainerTooltipText(ContainerTooltipTextEvent event) {
        this.previewRenderer.cancelTooltip(event);
    }

    @EventHandler
    private void onTooltipData(TooltipDataEvent event) {
        this.previewRenderer.onTooltipData(event);
    }

    @EventHandler
    private void onScreenClose(ScreenCloseEvent event) {
        this.previewRenderer.reset();
    }

    @EventHandler
    private void onIsPointOverSlot(IsPointOverSlotEvent event) {
        this.previewRenderer.isPointOverSlot(event);
    }

    @EventHandler
    private void onDrawMouseoverTooltip(DrawMouseoverTooltipEvent event) {
        this.previewRenderer.drawPinned(event);
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        this.markRenderer.onInteractBlock(event);
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        this.markRenderer.onOpenScreen(event);
    }

    @EventHandler
    private void onWorldEntityRender(WorldEntityRenderEvent event) {
        if (this.containerMark.get() && this.isReady()) {
            this.markRenderer.renderWorld(event);
        }
    }

    private void persistCacheChanged(Boolean value) {
        if (Boolean.FALSE.equals(value)) {
            this.markRenderer.deleteCache();
            this.refreshAll();
        }
    }

    private void refreshAll() {
        this.markRenderer.clearAll();
        this.highlightRenderer.reset();
        this.previewRenderer.reset();
    }

    @Override
    public void onDeactivate() {
        this.markRenderer.saveCache();
        this.refreshAll();
    }
}