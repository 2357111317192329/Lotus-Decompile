/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 *  meteordevelopment.meteorclient.events.game.GameJoinedEvent
 *  meteordevelopment.meteorclient.events.game.GameLeftEvent
 *  meteordevelopment.meteorclient.events.game.OpenScreenEvent
 *  meteordevelopment.meteorclient.events.render.Render2DEvent
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.DoubleSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.meteorclient.utils.world.Dir
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.client.render.item.ItemRenderState
 *  net.minecraft.inventory.Inventory
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.screen.slot.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.world.BlockRenderView
 *  net.minecraft.world.BlockView
 *  net.minecraft.world.World
 *  net.minecraft.block.ChestBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.hit.HitResult
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.block.entity.ChestBlockEntity
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.enums.ChestType
 *  net.minecraft.state.property.Property
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.hit.BlockHitResult
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.render.OverlayTexture
 *  net.minecraft.client.gui.screen.ingame.HandledScreen
 *  net.minecraft.client.render.WorldRenderer
 *  net.minecraft.util.math.RotationAxis
 *  net.minecraft.item.ItemDisplayContext
 *  org.joml.Matrix3x2fStack
 *  org.joml.Quaternionfc
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.PosUtils;

import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import com.xiaohe66.mc.meteor.lotus.bo.DoublePos;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageWarp;
import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.state.property.Property;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionfc;

public class PreviewTool extends BaseModule {
    private static final int SLOT_SIZE = 18;
    private static final int MARGIN = 2;
    private final SettingGroup iconGroup = settings.createGroup("盒子图标");
    public final Setting<Boolean> kitIcon = iconGroup.add(new BoolSetting.Builder()
        .name("盒子图标")
        .description("显示盒子数量最多的物品(以组为单位)")
        .defaultValue(true)
        .build());
    private final Setting<Double> iconScale = iconGroup.add(new DoubleSetting.Builder()
        .name("图标比例")
        .description("盒子图标比例")
        .defaultValue(0.4)
        .sliderRange(0.1, 1.0)
        .build());
    private final Setting<Integer> iconOffsetX = iconGroup.add(new IntSetting.Builder()
        .name("图标偏移量X")
        .description("偏移量")
        .defaultValue(7)
        .sliderRange(-20, 20)
        .build());
    private final Setting<Integer> iconOffsetY = iconGroup.add(new IntSetting.Builder()
        .name("图标偏移量Y")
        .description("偏移量")
        .defaultValue(7)
        .sliderRange(-20, 20)
        .build());
    private final SettingGroup containerMarkGroup = settings.createGroup("容器标识");
    public final Setting<Boolean> containerMark = containerMarkGroup.add(new BoolSetting.Builder()
        .name("容器标识")
        .description("展示打开过的容器中占比最高的物品，在容器朝向玩家的那一面显示图标")
        .defaultValue(true)
        .build());
    private final Setting<Double> markScale = containerMarkGroup.add(new DoubleSetting.Builder()
        .name("标识大小")
        .description("容器标识图标的显示大小")
        .defaultValue(1.0)
        .sliderRange(0.5, 2.0)
        .build());
    private final Setting<Integer> markDistance = containerMarkGroup.add(new IntSetting.Builder()
        .name("标识距离")
        .description("超过此距离不显示容器标识")
        .defaultValue(24)
        .min(4)
        .sliderMax(64)
        .build());
    private final Setting<Boolean> highlightChests = containerMarkGroup.add(new BoolSetting.Builder()
        .name("高亮箱子")
        .description("高亮显示包含快捷栏选中物品的箱子")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> highlightFrames = containerMarkGroup.add(new BoolSetting.Builder()
        .name("高亮展示框")
        .description("高亮显示包含快捷栏选中物品的展示框")
        .defaultValue(true)
        .build());
    private final Setting<Integer> highlightDistance = containerMarkGroup.add(new IntSetting.Builder()
        .name("高亮距离")
        .description("超过此距离不显示高亮效果")
        .defaultValue(64)
        .min(4)
        .sliderMax(128)
        .build());
    public final Setting<SettingColor> highlightColor = sgGeneral.add(new ColorSetting.Builder()
        .name("高亮颜色")
        .defaultValue(new Color(255, 215, 0, 64))
        .build());
    private final Setting<Integer> clearDistance = containerMarkGroup.add(new IntSetting.Builder()
        .name("清除距离")
        .description("超过此距离时清除缓存的容器标识")
        .defaultValue(256)
        .min(64)
        .sliderMax(1024)
        .build());
    private final SettingGroup kitSpreadGroup = settings.createGroup("盒子平铺");
    public final Setting<Boolean> kitSpread = kitSpreadGroup.add(new BoolSetting.Builder()
        .name("盒子平铺")
        .description("将kit在界面中平铺显示")
        .defaultValue(true)
        .build());
    private final Setting<Integer> top = kitSpreadGroup.add(new IntSetting.Builder()
        .name("上边距")
        .description("上边距")
        .defaultValue(6)
        .min(0)
        .sliderMax(100)
        .build());
    private final Setting<Integer> left = kitSpreadGroup.add(new IntSetting.Builder()
        .name("左边距")
        .description("左边距")
        .defaultValue(6)
        .min(0)
        .sliderMax(500)
        .build());
    private final Setting<Integer> marge = kitSpreadGroup.add(new IntSetting.Builder()
        .name("间距")
        .description("间距")
        .defaultValue(4)
        .min(0)
        .sliderMax(10)
        .build());
    private final Setting<Double> scale = kitSpreadGroup.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("平铺区域的缩放比例")
        .defaultValue(1.0)
        .sliderRange(0.5, 1.5)
        .build());
    private final Setting<Boolean> compact = kitSpreadGroup.add(new BoolSetting.Builder()
        .name("紧凑模式")
        .description("将相同物品合并显示，节省空间")
        .defaultValue(true)
        .build());
    private final Setting<Integer> backgroundAlpha = kitSpreadGroup.add(new IntSetting.Builder()
        .name("背景透明度")
        .description("盒子背景的不透明度")
        .defaultValue(30)
        .min(0)
        .sliderMax(255)
        .build());
    private int scrollOffset;
    private int lastSyncId;
    private final Map<BlockPos, StorageWarp> blockPosMarkMap;
    private final Map<DoublePos, StorageWarp> doublePosMarkMap;
    private final Map<ItemBo, ItemRenderState> itemRenderStateMap;
    private final List<BlockPos> highlightBlockPosList;
    private final List<DoublePos> highlightDoublePosList;
    private final Set<ItemFrameEntity> highlightItemFrames;
    private int selectedSlot;
    private BlockPos currentContainerPos;

    public PreviewTool() {
        super("预览器", "盒子小图标预览、盒子内容平铺、容器标识");
        this.scrollOffset = 0;
        this.lastSyncId = 0;
        this.blockPosMarkMap = new HashMap<BlockPos, StorageWarp>();
        this.doublePosMarkMap = new HashMap<DoublePos, StorageWarp>();
        this.itemRenderStateMap = new HashMap<ItemBo, ItemRenderState>();
        this.highlightBlockPosList = new ArrayList<BlockPos>();
        this.highlightDoublePosList = new ArrayList<DoublePos>();
        this.highlightItemFrames = new HashSet<ItemFrameEntity>();
        this.selectedSlot = -1;
    }

    @EventHandler
    private void onGameLeftEvent(GameLeftEvent event) {
        this.clearAll();
    }

    @EventHandler
    private void onGameJoinedEvent(GameJoinedEvent event) {
        this.clearAll();
    }

    @EventHandler
    private void onOpenScreen(ScreenRenderEvent event) {
        if (!this.kitSpread.get() || !(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        List<ShulkerBoxReader> readerList = this.collectReaders();
        if (readerList.isEmpty()) {
            return;
        }
        if (this.lastSyncId != this.mc.player.currentScreenHandler.syncId) {
            this.scrollOffset = 0;
            this.lastSyncId = this.mc.player.currentScreenHandler.syncId;
        }
        DrawContext drawContext = event.getDrawContext();
        drawContext.createNewRootLayer();
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        float scaleFactor = this.scale.get().floatValue();
        matrix3x2fStack.scale(scaleFactor, scaleFactor);
        float scrollOffsetScaled = (float)this.scrollOffset / scaleFactor;
        int leftPos = this.left.get();
        int topPos = this.top.get() + (int)scrollOffsetScaled;
        ItemStack tooltipStack = ItemStack.EMPTY;
        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = this.compact.get() != false ? reader.getCondensed() : new ArrayList<ItemStack>(reader.getStacks());
            if (!this.compact.get()) {
                while (stacks.size() < 27) {
                    stacks.add(ItemStack.EMPTY);
                }
            }
            int itemCount = this.compact.get() != false ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : stacks.size();
            int rowCount = Math.max(1, (itemCount - 1) / 9 + 1);
            int colCount = MathHelper.clamp(itemCount, 1, 9);
            int boxWidth = colCount * 18 + 4;
            int boxHeight = rowCount * 18 + 4;
            int boxColor = reader.getColor();
            int alpha = this.backgroundAlpha.get();
            int bgColor = alpha << 24;
            drawContext.fill(leftPos, topPos, leftPos + boxWidth, topPos + boxHeight, bgColor);
            drawContext.fill(leftPos, topPos - 1, leftPos + boxWidth, topPos, boxColor);
            int index = 0;
            for (ItemStack stack : stacks) {
                if (this.compact.get() && stack.isEmpty()) continue;
                int col = index % 9;
                int row = index / 9;
                int x = leftPos + 2 + col * 18;
                int y = topPos + 2 + row * 18;
                this.drawStack(drawContext, event.getTextRenderer(), stack, x, y);
                if (this.isMouseOverSlot(event.getMouseX(), event.getMouseY(), x, y, scaleFactor)) {
                    tooltipStack = stack;
                }
                ++index;
            }
            if (reader.getOriginItemStackCount() > 1) {
                String countText = "x" + reader.getOriginItemStackCount();
                int countX = leftPos + boxWidth + 2;
                Objects.requireNonNull(this.mc.textRenderer);
                int countY = topPos + boxHeight - 9 - 2;
                drawContext.drawText(event.getTextRenderer(), countText, countX, countY, Color.GREEN.getPacked(), true);
            }
            topPos += boxHeight + this.marge.get();
        }
        if (!tooltipStack.isEmpty()) {
            float inverseScale = 1.0f / scaleFactor;
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.scale(inverseScale, inverseScale);
            drawContext.drawItemTooltip(event.getTextRenderer(), tooltipStack, event.getMouseX(), event.getMouseY());
            matrix3x2fStack.popMatrix();
        }
        matrix3x2fStack.popMatrix();
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (!this.kitSpread.get() || !(this.mc.currentScreen instanceof HandledScreen)) {
            return;
        }
        List<ShulkerBoxReader> readerList = this.collectReaders();
        if (readerList.isEmpty()) {
            return;
        }
        float totalHeight = 0.0f;
        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = this.compact.get() != false ? reader.getCondensed() : reader.getStacks();
            int itemCount = this.compact.get() != false ? (int)stacks.stream().filter(s -> !s.isEmpty()).count() : 27;
            int rowCount = Math.max(1, (itemCount - 1) / 9 + 1);
            totalHeight += (float)(rowCount * 18 + 4 + this.marge.get());
        }
        float scaleFactor = this.scale.get().floatValue();
        float maxScroll = Math.min(-(totalHeight += (float)this.marge.get()) + (float)this.mc.getWindow().getScaledHeight() / scaleFactor, 0.0f);
        this.scrollOffset = (int)MathHelper.clamp((double)this.scrollOffset + Math.ceil(event.getVerticalAmount()) * 15.0, (double)maxScroll, (double)0.0);
    }

    @EventHandler
    private void onHandledScreenRenderEvent(HandledScreenRenderEvent event) {
        Screen screen = this.mc.currentScreen;
        if (!(screen instanceof HandledScreen)) {
            return;
        }
        HandledScreen handledScreen = (HandledScreen)screen;
        ScreenHandler screenHandler = handledScreen.getScreenHandler();
        if (this.currentContainerPos != null) {
            ItemStack[] slotStacks = new ItemStack[this.getScreenMainSize()];
            for (int i = 0; i < slotStacks.length; ++i) {
                ItemStack stack = screenHandler.getSlot(i).getStack();
                slotStacks[i] = stack == null ? ItemStack.EMPTY : stack;
            }
            StorageWarp storageWarp = new StorageWarp((ItemStack[])slotStacks);
            List<BlockPos> chestPositions = PosUtils.getChestPositions(this.currentContainerPos);
            if (chestPositions.size() > 1) {
                DoublePos doublePos = new DoublePos(chestPositions.get(0), chestPositions.get(1));
                this.doublePosMarkMap.put(doublePos, storageWarp);
            } else {
                this.blockPosMarkMap.put(chestPositions.getFirst(), storageWarp);
            }
        }
        if (this.kitIcon.get()) {
            for (Slot slot : screenHandler.slots) {
                ItemStack slotStack = slot.getStack();
                if (slotStack == null || !HeItemUtils.isShulkerBox(slotStack.getItem())) continue;
                this.drawKitIcon(event.getDrawContext(), slotStack, slot.x, slot.y);
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent render2DEvent) {
        if (!(this.mc.currentScreen instanceof HandledScreen)) {
            this.currentContainerPos = null;
        }
        if (this.kitIcon.get() && !(this.mc.currentScreen instanceof HandledScreen)) {
            this.drawKitIcons(render2DEvent.drawContext);
        }
        if (!this.isReady()) {
            return;
        }
        int selected = this.mc.player.getInventory().getSelectedSlot();
        if (selected == this.selectedSlot) {
            return;
        }
        this.selectedSlot = selected;
        this.updateHighlights();
    }

    @EventHandler
    private void onRender3D(Render3DEvent render3DEvent) {
        this.clearOutdatedMarks();
        if (this.containerMark.get() && this.isReady()) {
            Vec3d cameraPos = HePosUtils.getCameraPos();
            if (this.highlightChests.get()) {
                for (BlockPos blockPos : this.highlightBlockPosList) {
                    this.renderChestHighlight(render3DEvent, blockPos, cameraPos);
                }
                for (DoublePos doublePos : this.highlightDoublePosList) {
                    this.renderDoubleChestHighlight(render3DEvent, doublePos, cameraPos);
                }
            }
            if (this.highlightFrames.get()) {
                this.renderItemFrameHighlight(render3DEvent, cameraPos);
            }
        }
    }

    private void drawKitIcons(DrawContext drawContext) {
        int screenWidth = this.mc.getWindow().getScaledWidth();
        int screenHeight = this.mc.getWindow().getScaledHeight();
        int centerX = 182;
        int centerY = 22;
        int x = (screenWidth - centerX) / 2 + 3;
        int y = screenHeight - centerY + 3;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (stack == null || !HeItemUtils.isShulkerBox(stack.getItem())) continue;
            int iconX = x + i * 20;
            int iconY = y;
            this.drawKitIcon(drawContext, stack, iconX, iconY);
        }
    }

    private void drawKitIcon(DrawContext drawContext, ItemStack kitItemStack, int x, int y) {
        ShulkerBoxReader shulkerReader = new ShulkerBoxReader(kitItemStack);
        ItemStack maximumItemStack = shulkerReader.getMaximumItem();
        if (maximumItemStack.isEmpty()) {
            return;
        }
        float scaleFactor = this.iconScale.get().floatValue();
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate((float)(x + this.iconOffsetX.get()), (float)(y + this.iconOffsetY.get()));
        matrix3x2fStack.scale(scaleFactor, scaleFactor);
        drawContext.drawItem(maximumItemStack, 0, 0);
        matrix3x2fStack.popMatrix();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate(-2.0f, 0.0f);
        int countOffsetY = 2;
        int countHeight = 12;
        int countBarHeight = (int)((double)maximumItemStack.getCount() * 1.0 / (double)maximumItemStack.getMaxCount() / 27.0 * (double)countHeight);
        int barX1 = x + 16;
        int barX2 = x + 17;
        int barY1 = y + countOffsetY;
        int barY2 = barY1 + countHeight;
        int countY = barY2 - countBarHeight;
        drawContext.fill(barX1, barY1, barX2, barY2, -1);
        drawContext.fill(barX1, countY, barX2, barY2, -16711936);
        matrix3x2fStack.popMatrix();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent openScreenEvent) {
        Screen screen = openScreenEvent.screen;
        if (screen instanceof HandledScreen) {
            HandledScreen handledScreen = (HandledScreen)screen;
            if (handledScreen.getScreenHandler() instanceof PlayerScreenHandler) {
                this.currentContainerPos = null;
                return;
            }
            HitResult hitResult = this.mc.crosshairTarget;
            if (hitResult instanceof BlockHitResult) {
                BlockPos containerPos = ((BlockHitResult)hitResult).getBlockPos();
                if (HeBlockUtils.isContainer(containerPos)) {
                    this.currentContainerPos = containerPos;
                }
            }
        }
    }

    @EventHandler
    private void onWorldEntityRender(WorldEntityRenderEvent event) {
        if (this.containerMark.get() && this.isReady()) {
            this.renderContainerMarks(event);
            this.renderDoubleChestMarks(event);
        }
    }

    private void renderContainerMarks(WorldEntityRenderEvent event) {
        Vec3d cameraPos = event.getPos();
        this.blockPosMarkMap.entrySet().removeIf(entry -> {
            BlockPos blockPos = (BlockPos)entry.getKey();
            if (!HeBlockUtils.isContainer(blockPos)) {
                return true;
            }
            double distance = cameraPos.distanceTo(blockPos.toCenterPos());
            return distance > (double)this.clearDistance.get();
        });
        for (Map.Entry<BlockPos, StorageWarp> entry : this.blockPosMarkMap.entrySet()) {
            BlockPos blockPos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = this.findBestDirection(blockPos, cameraPos);
            this.renderItemFrameMark(event, blockPos, clickDirection, cameraPos, storageWarp);
        }
    }

    private void renderDoubleChestMarks(WorldEntityRenderEvent event) {
        Vec3d cameraPos = event.getPos();
        this.doublePosMarkMap.entrySet().removeIf(entry -> {
            DoublePos doublePos = (DoublePos)entry.getKey();
            if (!HeBlockUtils.isContainer(doublePos.getPos1()) || !HeBlockUtils.isContainer(doublePos.getPos2())) {
                return true;
            }
            BlockPos pos1 = doublePos.getPos1();
            BlockPos pos2 = doublePos.getPos2();
            Vec3d midPos = new Vec3d((double)(pos1.getX() + pos2.getX()) / 2.0 + 0.5, (double)(pos1.getY() + pos2.getY()) / 2.0 + 0.5, (double)(pos1.getZ() + pos2.getZ()) / 2.0 + 0.5);
            double distance = cameraPos.distanceTo(midPos);
            return distance > (double)this.clearDistance.get();
        });
        for (Map.Entry<DoublePos, StorageWarp> entry : this.doublePosMarkMap.entrySet()) {
            DoublePos doublePos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = null;
            BlockPos markBlockPos = null;
            double bestDistance = -1.7976931348623157E308;
            for (BlockPos cornerPos : Arrays.asList(doublePos.getPos1(), doublePos.getPos2())) {
                Vec3d cornerCenter = new Vec3d((double)cornerPos.getX() + 0.5, (double)cornerPos.getY() + 0.5, (double)cornerPos.getZ() + 0.5);
                for (Direction direction : Direction.values()) {
                    double score = this.getDirectionScore(cornerCenter, cameraPos, direction);
                    if (!(score > 0.0) || !(score > bestDistance) || this.isDirectionBlocked(cornerPos, direction)) continue;
                    bestDistance = score;
                    clickDirection = direction;
                    markBlockPos = cornerPos;
                }
            }
            if (clickDirection == null) continue;
            this.renderItemFrameMark(event, markBlockPos, clickDirection, cameraPos, storageWarp);
        }
    }

    private void renderItemFrameMark(WorldEntityRenderEvent event, BlockPos blockPos, Direction clickDirection, Vec3d cameraPos, StorageWarp storageWarp) {
        Vec3d markPos = this.getFaceMarkPos(blockPos, clickDirection);
        double distance = cameraPos.distanceTo(markPos);
        if (distance > (double)this.markDistance.get()) {
            return;
        }
        ItemStack iconStack;
        ItemStack overlayStack = null;
        ItemBo bestItem = storageWarp.getBestItem();
        if (bestItem == null) {
            iconStack = Items.BARRIER.getDefaultStack();
        } else if (HeItemUtils.isShulkerBox(bestItem.getItem())) {
            iconStack = bestItem.getItem().getDefaultStack();
            ItemStack containerStack = storageWarp.getStack();
            if (!containerStack.isEmpty()) {
                overlayStack = containerStack;
            }
        } else {
            iconStack = storageWarp.getStack();
        }
        MatrixStack matrixStack = event.getMatrixStack();
        matrixStack.push();
        matrixStack.translate(markPos.x - cameraPos.x, markPos.y - cameraPos.y, markPos.z - cameraPos.z);
        float rotationX;
        float rotationY;
        if (clickDirection.getAxis().isHorizontal()) {
            rotationX = 0.0f;
            rotationY = 180.0f - clickDirection.getPositiveHorizontalDegrees();
        } else {
            rotationX = -90 * clickDirection.getDirection().offset();
            rotationY = 180.0f;
        }
        matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        int light = WorldRenderer.getLightmapCoordinates((BlockRenderView)this.mc.world, (BlockPos)blockPos.offset(clickDirection));
        float scale = 0.5f * this.markScale.get().floatValue();
        this.renderItemIcon(event, matrixStack, iconStack, light, scale);
        if (overlayStack != null) {
            matrixStack.translate(-0.16, -0.16, 0.02);
            this.renderItemIcon(event, matrixStack, overlayStack, light, scale * 0.55f);
        }
        matrixStack.pop();
    }

    private void renderItemIcon(WorldEntityRenderEvent event, MatrixStack matrixStack, ItemStack stack, int light, float scale) {
        matrixStack.push();
        matrixStack.scale(scale, scale, scale);
        this.getItemRenderState(stack).render(matrixStack, event.getCommandQueue(), light, OverlayTexture.DEFAULT_UV, 0);
        matrixStack.pop();
    }

    private ItemRenderState getItemRenderState(ItemStack stack) {
        return this.itemRenderStateMap.computeIfAbsent(new ItemBo(stack), itemBo -> {
            ItemRenderState renderState = new ItemRenderState();
            this.mc.getItemModelManager().clearAndUpdate(renderState, stack, ItemDisplayContext.FIXED, (World)this.mc.world, null, 0);
            return renderState;
        });
    }

    private Direction findBestDirection(BlockPos blockPos, Vec3d cameraPos) {
        Direction direction;
        Vec3d centerPos = new Vec3d((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5);
        List sortedDirections = Arrays.stream(Direction.values()).sorted((dir1, dir2) -> Double.compare(this.getDirectionScore(centerPos, cameraPos, (Direction)dir2), this.getDirectionScore(centerPos, cameraPos, (Direction)dir1))).toList();
        Iterator iterator = sortedDirections.iterator();
        while (iterator.hasNext() && !(this.getDirectionScore(centerPos, cameraPos, direction = (Direction)iterator.next()) <= 0.0)) {
            if (this.isDirectionBlocked(blockPos, direction)) continue;
            return direction;
        }
        return (Direction)sortedDirections.get(0);
    }

    private double getDirectionScore(Vec3d cameraPos, Vec3d targetPos, Direction direction) {
        Vec3d faceCenter = this.getFaceCenter(cameraPos, direction);
        Vec3d offset = targetPos.subtract(faceCenter);
        Vec3i directionVec = direction.getVector();
        return offset.x * (double)directionVec.getX() + offset.y * (double)directionVec.getY() + offset.z * (double)directionVec.getZ();
    }

    private Vec3d getFaceCenter(Vec3d pos, Direction direction) {
        return switch (direction) {
            default -> throw new MatchException(null, null);
            case Direction.UP -> pos.add(0.0, 0.51, 0.0);
            case Direction.DOWN -> pos.add(0.0, -0.51, 0.0);
            case Direction.NORTH -> pos.add(0.0, 0.0, -0.51);
            case Direction.SOUTH -> pos.add(0.0, 0.0, 0.51);
            case Direction.WEST -> pos.add(-0.51, 0.0, 0.0);
            case Direction.EAST -> pos.add(0.51, 0.0, 0.0);
        };
    }

    private boolean isDirectionBlocked(BlockPos blockPos, Direction direction) {
        BlockPos adjacentPos = blockPos.offset(direction);
        BlockState state = this.mc.world.getBlockState(adjacentPos);
        if (state.isAir()) {
            return false;
        }
        if (state.isOpaque() && state.isFullCube((BlockView)this.mc.world, adjacentPos)) {
            return true;
        }
        BlockEntity blockEntity = this.mc.world.getBlockEntity(adjacentPos);
        return blockEntity instanceof Inventory;
    }

    private Vec3d getFaceMarkPos(BlockPos blockPos, Direction direction) {
        double x = (double)blockPos.getX() + 0.5;
        double y = (double)blockPos.getY() + 0.5;
        double z = (double)blockPos.getZ() + 0.5;
        switch (direction) {
            case UP: {
                y += 0.51;
                break;
            }
            case DOWN: {
                y -= 0.51;
                break;
            }
            case NORTH: {
                z -= 0.51;
                break;
            }
            case SOUTH: {
                z += 0.51;
                break;
            }
            case WEST: {
                x -= 0.51;
                break;
            }
            case EAST: {
                x += 0.51;
            }
        }
        return new Vec3d(x, y, z);
    }

    private void clearOutdatedMarks() {
        Vec3d cameraPos = HePosUtils.getCameraPos();
        int maxDistance = this.clearDistance.get();
        this.blockPosMarkMap.entrySet().removeIf(entry -> {
            BlockPos blockPos = (BlockPos)entry.getKey();
            double distance = cameraPos.distanceTo(blockPos.toCenterPos());
            return distance > (double)maxDistance;
        });
        this.doublePosMarkMap.entrySet().removeIf(entry -> {
            DoublePos doublePos = (DoublePos)entry.getKey();
            BlockPos pos1 = doublePos.getPos1();
            BlockPos pos2 = doublePos.getPos2();
            Vec3d midPos = new Vec3d((double)(pos1.getX() + pos2.getX()) / 2.0 + 0.5, (double)(pos1.getY() + pos2.getY()) / 2.0 + 0.5, (double)(pos1.getZ() + pos2.getZ()) / 2.0 + 0.5);
            double distance = cameraPos.distanceTo(midPos);
            return distance > (double)maxDistance;
        });
    }

    private void updateHighlights() {
        ItemStack selectedStack = this.mc.player.getInventory().getStack(this.selectedSlot);
        if (selectedStack == null || selectedStack.isEmpty()) {
            return;
        }
        ItemBo selectedItemBo = new ItemBo(selectedStack);
        for (Map.Entry<BlockPos, StorageWarp> entry : this.blockPosMarkMap.entrySet()) {
            StorageWarp storageWarp = entry.getValue();
            if (!storageWarp.contains(selectedItemBo)) continue;
            this.highlightBlockPosList.add(entry.getKey());
        }
        for (Map.Entry<DoublePos, StorageWarp> entry : this.doublePosMarkMap.entrySet()) {
            StorageWarp storageWarp = entry.getValue();
            if (!storageWarp.contains(selectedItemBo)) continue;
            this.highlightDoublePosList.add(entry.getKey());
        }
        int searchRange = this.markDistance.get();
        List itemFrames = this.mc.world.getEntitiesByClass(ItemFrameEntity.class, this.mc.player.getBoundingBox().expand((double)searchRange), itemFrame -> true);
        for (ItemFrameEntity itemFrame : (List<ItemFrameEntity>)itemFrames) {
            ItemStack frameStack = itemFrame.getHeldItemStack();
            if (frameStack == null || frameStack.isEmpty() || !selectedItemBo.equals(new ItemBo(frameStack))) continue;
            this.highlightItemFrames.add(itemFrame);
        }
    }

    private void renderItemFrameHighlight(Render3DEvent render3DEvent, Vec3d cameraPos) {
        int maxDistance = this.highlightDistance.get();
        for (ItemFrameEntity itemFrame : this.highlightItemFrames) {
            double distance;
            BlockPos blockPos;
            if (!itemFrame.isAlive() || (distance = cameraPos.distanceTo((blockPos = itemFrame.getBlockPos()).toCenterPos())) > (double)maxDistance) continue;
            Box box = itemFrame.getBoundingBox();
            render3DEvent.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, (Color)this.highlightColor.get(), (Color)this.highlightColor.get(), ShapeMode.Both, 0);
        }
    }

    private void renderChestHighlight(Render3DEvent render3DEvent, BlockPos blockPos, Vec3d cameraPos) {
        if (!HeBlockUtils.isContainer(blockPos)) {
            return;
        }
        double distance = cameraPos.distanceTo(blockPos.toCenterPos());
        if (distance > (double)this.highlightDistance.get()) {
            return;
        }
        double minX = blockPos.getX();
        double minY = blockPos.getY();
        double minZ = blockPos.getZ();
        double maxX = blockPos.getX() + 1;
        double maxY = blockPos.getY() + 1;
        double maxZ = blockPos.getZ() + 1;
        BlockEntity blockEntity = this.mc.world.getBlockEntity(blockPos);
        int lineWidth = 0;
        if (blockEntity instanceof ChestBlockEntity) {
            double inset = 0.0625;
            minX += inset;
            minZ += inset;
            maxX -= inset;
            maxY -= inset * 2.0;
            maxZ -= inset;
        }
        render3DEvent.renderer.box(minX, minY, minZ, maxX, maxY, maxZ, (Color)this.highlightColor.get(), (Color)this.highlightColor.get(), ShapeMode.Both, lineWidth);
    }

    private void renderDoubleChestHighlight(Render3DEvent render3DEvent, DoublePos doublePos, Vec3d cameraPos) {
        BlockPos pos1 = doublePos.getPos1();
        BlockPos pos2 = doublePos.getPos2();
        if (!HeBlockUtils.isContainer(pos1) || !HeBlockUtils.isContainer(pos2)) {
            return;
        }
        double midX = (double)(pos1.getX() + pos2.getX()) / 2.0 + 0.5;
        double midY = (double)(pos1.getY() + pos2.getY()) / 2.0 + 0.5;
        double midZ = (double)(pos1.getZ() + pos2.getZ()) / 2.0 + 0.5;
        double distance = cameraPos.distanceTo(new Vec3d(midX, midY, midZ));
        if (distance > (double)this.highlightDistance.get()) {
            return;
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        BlockEntity blockEntity = this.mc.world.getBlockEntity(pos1);
        int lineWidth = 0;
        if (blockEntity instanceof ChestBlockEntity) {
            double inset = 0.0625;
            BlockState state = this.mc.world.getBlockState(pos1);
            if (state.getBlock() instanceof ChestBlock && state.get((Property)ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                Direction facing = (Direction)state.get((Property)ChestBlock.FACING);
                lineWidth = Dir.get((Direction)facing);
            }
            if (Dir.isNot(lineWidth, (byte)32)) {
                minX += inset;
            }
            if (Dir.isNot(lineWidth, (byte)8)) {
                minZ += inset;
            }
            if (Dir.isNot(lineWidth, (byte)64)) {
                maxX -= inset;
            }
            maxY -= inset * 2.0;
            if (Dir.isNot(lineWidth, (byte)16)) {
                maxZ -= inset;
            }
        }
        render3DEvent.renderer.box(minX, minY, minZ, maxX, maxY, maxZ, (Color)this.highlightColor.get(), (Color)this.highlightColor.get(), ShapeMode.Both, lineWidth);
    }

    private List<ShulkerBoxReader> collectReaders() {
        ArrayList<ShulkerBoxReader> readerList = new ArrayList<ShulkerBoxReader>();
        for (ItemStack boxItemStack : HeInvUtils.findAndMargeShulkerBox()) {
            ShulkerBoxReader reader = new ShulkerBoxReader(boxItemStack);
            if (reader.isEmpty()) continue;
            readerList.add(reader);
        }
        return readerList;
    }

    private void drawStack(DrawContext drawContext, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        drawContext.drawItem(stack, x, y);
        if (stack.getCount() > 999) {
            String countText = "%.1fk".formatted(new Object[]{Float.valueOf((float)stack.getCount() / 1000.0f)});
            drawContext.drawStackOverlay(textRenderer, stack, x, y, countText);
        } else {
            drawContext.drawStackOverlay(textRenderer, stack, x, y);
        }
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY, float scale) {
        int scaledX = (int)((float)mouseX / scale);
        int scaledY = (int)((float)mouseY / scale);
        return scaledX >= slotX && scaledX < slotX + 18 && scaledY >= slotY && scaledY < slotY + 18;
    }

    private void clearAll() {
        this.blockPosMarkMap.clear();
        this.doublePosMarkMap.clear();
        this.itemRenderStateMap.clear();
        this.selectedSlot = -1;
        this.currentContainerPos = null;
        this.clearHighlights();
    }

    private void clearHighlights() {
        this.highlightBlockPosList.clear();
        this.highlightDoublePosList.clear();
        this.highlightItemFrames.clear();
    }

    public void onDeactivate() {
        this.clearAll();
    }

    static class PreviewToolDirectionSwitchMap {
        static final /* synthetic */ int[] a;

        static {
            a = new int[Direction.values().length];
            try {
                PreviewToolDirectionSwitchMap.a[Direction.UP.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                PreviewToolDirectionSwitchMap.a[Direction.DOWN.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                PreviewToolDirectionSwitchMap.a[Direction.NORTH.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                PreviewToolDirectionSwitchMap.a[Direction.SOUTH.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                PreviewToolDirectionSwitchMap.a[Direction.WEST.ordinal()] = 5;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                PreviewToolDirectionSwitchMap.a[Direction.EAST.ordinal()] = 6;
            }
            catch (NoSuchFieldError noSuchFieldError) {
            }
        }
    }
}
