/*
 * Port of Lotus 19.5 aM (container highlight renderer, decompiled
 * bytecode-accurate).  Highlights the chests / item frames whose contents
 * match the item currently selected in the hotbar.  The highlight lists are
 * refreshed whenever the selected hotbar slot changes.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.DoublePos;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageWarp;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ContainerHighlightRenderer {
    private final ContainerMarkRenderer markRenderer;
    private final Setting<Boolean> highlightChests;
    private final Setting<Boolean> highlightFrames;
    private final Setting<Integer> highlightDistance;
    private final Setting<SettingColor> highlightColor;
    private final Setting<Integer> markDistance;
    private final List<BlockPos> highlightBlockPosList = new ArrayList<>();
    private final List<DoublePos> highlightDoublePosList = new ArrayList<>();
    private final Set<ItemFrameEntity> highlightItemFrames = new HashSet<>();
    private int selectedSlot = -1;

    public ContainerHighlightRenderer(ContainerMarkRenderer markRenderer, Setting<Boolean> highlightChests, Setting<Boolean> highlightFrames, Setting<Integer> highlightDistance, Setting<SettingColor> highlightColor, Setting<Integer> markDistance) {
        this.markRenderer = markRenderer;
        this.highlightChests = highlightChests;
        this.highlightFrames = highlightFrames;
        this.highlightDistance = highlightDistance;
        this.highlightColor = highlightColor;
        this.markDistance = markDistance;
    }

    public void render3D(Render3DEvent event) {
        int selected = MeteorClient.mc.player.getInventory().getSelectedSlot();
        if (selected != this.selectedSlot) {
            this.selectedSlot = selected;
            this.refresh();
        }
        Vec3d cameraPos = HePosUtils.getCameraPos();
        if (this.highlightChests.get()) {
            for (BlockPos blockPos : this.highlightBlockPosList) {
                this.renderChestHighlight(event, blockPos, cameraPos);
            }
            for (DoublePos doublePos : this.highlightDoublePosList) {
                this.renderDoubleChestHighlight(event, doublePos, cameraPos);
            }
        }
        if (this.highlightFrames.get()) {
            this.renderItemFrameHighlight(event, cameraPos);
        }
    }

    private void refresh() {
        this.clear();
        ItemStack selectedStack = MeteorClient.mc.player.getInventory().getStack(this.selectedSlot);
        if (selectedStack == null || selectedStack.isEmpty()) {
            return;
        }
        if (HeItemUtils.isShulkerBox(selectedStack.getItem())) {
            ShulkerBoxReader reader = new ShulkerBoxReader(selectedStack);
            if (!reader.isEmpty()) {
                selectedStack = reader.getMaximumItem();
            }
        }
        ItemBo selectedItemBo = new ItemBo(selectedStack);
        for (Map.Entry<BlockPos, StorageWarp> entry : this.markRenderer.getMarkMap().entrySet()) {
            if (entry.getValue().contains(selectedItemBo)) {
                this.highlightBlockPosList.add(entry.getKey());
            }
        }
        for (Map.Entry<DoublePos, StorageWarp> entry : this.markRenderer.getDoubleMarkMap().entrySet()) {
            if (entry.getValue().contains(selectedItemBo)) {
                this.highlightDoublePosList.add(entry.getKey());
            }
        }
        int searchRange = this.markDistance.get();
        List<ItemFrameEntity> itemFrames = MeteorClient.mc.world.getEntitiesByClass(ItemFrameEntity.class, MeteorClient.mc.player.getBoundingBox().expand((double)searchRange), itemFrame -> true);
        for (ItemFrameEntity itemFrame : itemFrames) {
            ItemStack frameStack = itemFrame.getHeldItemStack();
            if (frameStack == null || frameStack.isEmpty()) {
                continue;
            }
            if (HeItemUtils.isShulkerBox(frameStack.getItem())) {
                ShulkerBoxReader frameReader = new ShulkerBoxReader(frameStack);
                if (!frameReader.isEmpty()) {
                    frameStack = frameReader.getMaximumItem();
                }
            }
            if (selectedItemBo.equals(new ItemBo(frameStack))) {
                this.highlightItemFrames.add(itemFrame);
            }
        }
    }

    private void renderItemFrameHighlight(Render3DEvent render3DEvent, Vec3d cameraPos) {
        int maxDistance = this.highlightDistance.get();
        Iterator<ItemFrameEntity> iterator = this.highlightItemFrames.iterator();
        while (iterator.hasNext()) {
            ItemFrameEntity itemFrame = iterator.next();
            if (!itemFrame.isAlive()) {
                iterator.remove();
                continue;
            }
            double distance = cameraPos.distanceTo(itemFrame.getBlockPos().toCenterPos());
            if (distance > (double)maxDistance) {
                continue;
            }
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
        BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(blockPos);
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
        BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(pos1);
        int lineWidth = 0;
        if (blockEntity instanceof ChestBlockEntity) {
            double inset = 0.0625;
            BlockState state = MeteorClient.mc.world.getBlockState(pos1);
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

    /** Resets the selected slot so the highlight lists are rebuilt on the next 3D frame. */
    public void reset() {
        this.selectedSlot = -1;
        this.clear();
    }

    private void clear() {
        this.highlightBlockPosList.clear();
        this.highlightDoublePosList.clear();
        this.highlightItemFrames.clear();
    }
}