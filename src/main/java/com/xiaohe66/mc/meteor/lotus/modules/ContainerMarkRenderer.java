/*
 * Port of Lotus 19.5 aN (container mark renderer, decompiled
 * bytecode-accurate).  Tracks the containers the player opens and renders
 * the dominant item icon on the face of the container that points at the
 * camera.  Optionally persists the mark map to disk (NBT, mirroring 19.5 aP).
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.DoublePos;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageWarp;
import com.xiaohe66.mc.meteor.lotus.event.WorldEntityRenderEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.util.PosUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerMarkRenderer {
    private static final Logger log = LoggerFactory.getLogger(ContainerMarkRenderer.class);

    private final Setting<Double> markScale;
    private final Setting<Integer> markDistance;
    private final Setting<Boolean> persistCache;
    private final Map<BlockPos, StorageWarp> markMap = new HashMap<>();
    private final Map<DoublePos, StorageWarp> doubleMarkMap = new HashMap<>();
    private final Map<ItemBo, ItemStackRenderState> itemRenderStateMap = new HashMap<>();
    private BlockPos currentContainerPos;
    private BlockPos lastInteractPos;
    private long lastInteractTime = -1L;
    private int lastSyncId = -1;
    private int lastRevision = -1;

    public ContainerMarkRenderer(Setting<Double> markScale, Setting<Integer> markDistance, Setting<Boolean> persistCache) {
        this.markScale = markScale;
        this.markDistance = markDistance;
        this.persistCache = persistCache;
    }

    public Map<BlockPos, StorageWarp> getMarkMap() {
        return this.markMap;
    }

    public Map<DoublePos, StorageWarp> getDoubleMarkMap() {
        return this.doubleMarkMap;
    }

    /** Writes the mark maps to disk (persistable cache). */
    public void saveCache() {
        if (this.persistCache.get() && MeteorClient.mc.level != null) {
            if (!this.markMap.isEmpty() || !this.doubleMarkMap.isEmpty()) {
                save(LotusUtils.getServerId(), LotusUtils.getWorldId(), this.markMap, this.doubleMarkMap, MeteorClient.mc.level.registryAccess());
            }
        }
    }

    /** Loads the mark maps from disk (persistable cache). */
    public void loadCache() {
        if (this.persistCache.get() && MeteorClient.mc.level != null) {
            load(LotusUtils.getServerId(), LotusUtils.getWorldId(), this.markMap, this.doubleMarkMap, MeteorClient.mc.level.registryAccess());
        }
    }

    /** Deletes the on-disk cache. */
    public void deleteCache() {
        delete(LotusUtils.getServerId(), LotusUtils.getWorldId());
    }

    public void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        if (this.isValidContainerPos(pos)) {
            this.lastInteractPos = pos.immutable();
            this.lastInteractTime = MeteorClient.mc.level == null ? -1L : MeteorClient.mc.level.getGameTime();
        } else {
            this.lastInteractPos = null;
        }
    }

    public void onOpenScreen(OpenScreenEvent event) {
        Screen screen = event.screen;
        if (screen instanceof AbstractContainerScreen handledScreen) {
            if (handledScreen.getMenu() instanceof InventoryMenu || handledScreen.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu) {
                this.reset();
                return;
            }
            BlockPos pos = this.findContainerPos();
            if (pos != null) {
                this.currentContainerPos = pos.immutable();
                this.lastSyncId = -1;
            }
        }
    }

    private BlockPos findContainerPos() {
        if (this.lastInteractPos != null && MeteorClient.mc.level != null && MeteorClient.mc.level.getGameTime() - this.lastInteractTime <= 5L && this.isValidContainerPos(this.lastInteractPos)) {
            return this.lastInteractPos;
        } else {
            HitResult hitResult = MeteorClient.mc.hitResult;
            if (hitResult instanceof BlockHitResult) {
                BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                BlockPos pos = blockHitResult.getBlockPos();
                if (this.isValidContainerPos(pos)) {
                    return pos;
                }
            }
            return null;
        }
    }

    /** Clears the tracked container position when no container screen is open (called every 2D frame). */
    public void clearTick() {
        if (!(MeteorClient.mc.screen instanceof AbstractContainerScreen)) {
            this.reset();
        }
    }

    /** Reserved tick hook (empty in the reference implementation). */
    public void tick() {
    }

    public void reset() {
        this.currentContainerPos = null;
        this.lastInteractPos = null;
        this.lastSyncId = -1;
        this.lastRevision = -1;
    }

    /** Re-reads the container contents whenever the menu sync id / revision changes. */
    public void update(AbstractContainerMenu menu) {
        if (this.currentContainerPos != null) {
            if (menu.containerId != this.lastSyncId || menu.getStateId() != this.lastRevision) {
                this.lastSyncId = menu.containerId;
                this.lastRevision = menu.getStateId();
                int size = HeInvUtils.getScreenMainSize();
                ItemStack[] stacks = new ItemStack[size];
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = menu.getSlot(i).getItem();
                    stacks[i] = stack == null ? ItemStack.EMPTY : stack;
                }
                StorageWarp storageWarp = new StorageWarp(stacks);
                List<BlockPos> chestPositions = PosUtils.getChestPositions(this.currentContainerPos);
                if (chestPositions.size() > 1) {
                    this.doubleMarkMap.put(new DoublePos(chestPositions.get(0), chestPositions.get(1)), storageWarp);
                } else {
                    this.markMap.put(chestPositions.getFirst(), storageWarp);
                }
            }
        }
    }

    public void renderWorld(WorldEntityRenderEvent event) {
        this.renderSingleMarks(event);
        this.renderDoubleMarks(event);
    }

    private void renderSingleMarks(WorldEntityRenderEvent event) {
        Vec3 cameraPos = event.getPos();
        this.markMap.entrySet().removeIf(entry -> !this.isValidContainerPos(entry.getKey()));
        for (Map.Entry<BlockPos, StorageWarp> entry : this.markMap.entrySet()) {
            BlockPos blockPos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = this.findBestDirection(blockPos, cameraPos);
            this.renderMark(event, blockPos, clickDirection, cameraPos, storageWarp);
        }
    }

    private void renderDoubleMarks(WorldEntityRenderEvent event) {
        Vec3 cameraPos = event.getPos();
        this.doubleMarkMap.entrySet().removeIf(entry -> !this.isValidContainerPos(entry.getKey().getPos1()) || !this.isValidContainerPos(entry.getKey().getPos2()));
        for (Map.Entry<DoublePos, StorageWarp> entry : this.doubleMarkMap.entrySet()) {
            DoublePos doublePos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = null;
            BlockPos markBlockPos = null;
            double bestScore = -1.7976931348623157E308;
            for (BlockPos cornerPos : Arrays.asList(doublePos.getPos1(), doublePos.getPos2())) {
                Vec3 cornerCenter = new Vec3((double)cornerPos.getX() + 0.5, (double)cornerPos.getY() + 0.5, (double)cornerPos.getZ() + 0.5);
                for (Direction direction : Direction.values()) {
                    double score = this.getDirectionScore(cornerCenter, cameraPos, direction);
                    if (!(score > 0.0) || !(score > bestScore) || this.isDirectionBlocked(cornerPos, direction)) continue;
                    bestScore = score;
                    clickDirection = direction;
                    markBlockPos = cornerPos;
                }
            }
            if (clickDirection == null) continue;
            this.renderMark(event, markBlockPos, clickDirection, cameraPos, storageWarp);
        }
    }

    private void renderMark(WorldEntityRenderEvent event, BlockPos blockPos, Direction clickDirection, Vec3 cameraPos, StorageWarp storageWarp) {
        Vec3 markPos = this.getFaceMarkPos(blockPos, clickDirection);
        double distance = cameraPos.distanceTo(markPos);
        if (distance > (double)this.markDistance.get()) {
            return;
        }
        ItemStack iconStack;
        ItemStack overlayStack = null;
        ItemBo bestItem = storageWarp.getBestItem();
        if (bestItem == null) {
            iconStack = Items.BARRIER.getDefaultInstance();
        } else if (HeItemUtils.isShulkerBox(bestItem.getItem())) {
            iconStack = bestItem.getItem().getDefaultInstance();
            ItemStack containerStack = storageWarp.getStack();
            if (!containerStack.isEmpty()) {
                overlayStack = containerStack;
            }
        } else {
            iconStack = storageWarp.getStack();
        }
        PoseStack matrixStack = event.getMatrixStack();
        matrixStack.pushPose();
        matrixStack.translate(markPos.x - cameraPos.x, markPos.y - cameraPos.y, markPos.z - cameraPos.z);
        float rotationX;
        float rotationY;
        if (clickDirection.getAxis().isHorizontal()) {
            rotationX = 0.0f;
            rotationY = 180.0f - clickDirection.toYRot();
        } else {
            rotationX = -90 * clickDirection.getAxisDirection().getStep();
            rotationY = 180.0f;
        }
        matrixStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(rotationX));
        matrixStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(rotationY));
        int light = LevelRenderer.getLightCoords((BlockAndLightGetter)MeteorClient.mc.level, blockPos.relative(clickDirection));
        float scale = 0.5f * this.markScale.get().floatValue();
        this.renderItemIcon(event, matrixStack, iconStack, light, scale);
        if (overlayStack != null) {
            matrixStack.translate(-0.16, -0.16, 0.02);
            this.renderItemIcon(event, matrixStack, overlayStack, light, scale * 0.55f);
        }
        matrixStack.popPose();
    }

    private void renderItemIcon(WorldEntityRenderEvent event, PoseStack matrixStack, ItemStack stack, int light, float scale) {
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, scale);
        this.getItemRenderState(stack).submit(matrixStack, event.getCommandQueue(), light, OverlayTexture.NO_OVERLAY, 0);
        matrixStack.popPose();
    }

    private ItemStackRenderState getItemRenderState(ItemStack stack) {
        return this.itemRenderStateMap.computeIfAbsent(new ItemBo(stack), itemBo -> {
            ItemStackRenderState renderState = new ItemStackRenderState();
            MeteorClient.mc.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.FIXED, (Level)MeteorClient.mc.level, null, 0);
            return renderState;
        });
    }

    private Direction findBestDirection(BlockPos blockPos, Vec3 cameraPos) {
        Direction direction;
        Vec3 centerPos = new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5);
        List sortedDirections = Arrays.stream(Direction.values()).sorted((dir1, dir2) -> Double.compare(this.getDirectionScore(centerPos, cameraPos, (Direction)dir2), this.getDirectionScore(centerPos, cameraPos, (Direction)dir1))).toList();
        java.util.Iterator iterator = sortedDirections.iterator();
        while (iterator.hasNext() && !(this.getDirectionScore(centerPos, cameraPos, direction = (Direction)iterator.next()) <= 0.0)) {
            if (this.isDirectionBlocked(blockPos, direction)) continue;
            return direction;
        }
        return (Direction)sortedDirections.getFirst();
    }

    private double getDirectionScore(Vec3 centerPos, Vec3 cameraPos, Direction direction) {
        Vec3 faceCenter = this.getFaceCenter(centerPos, direction);
        Vec3 offset = cameraPos.subtract(faceCenter);
        Vec3i directionVec = direction.getUnitVec3i();
        return offset.x * (double)directionVec.getX() + offset.y * (double)directionVec.getY() + offset.z * (double)directionVec.getZ();
    }

    private Vec3 getFaceCenter(Vec3 pos, Direction direction) {
        return switch (direction) {
            case UP -> pos.add(0.0, 0.51, 0.0);
            case DOWN -> pos.add(0.0, -0.51, 0.0);
            case NORTH -> pos.add(0.0, 0.0, -0.51);
            case SOUTH -> pos.add(0.0, 0.0, 0.51);
            case WEST -> pos.add(-0.51, 0.0, 0.0);
            case EAST -> pos.add(0.51, 0.0, 0.0);
        };
    }

    private boolean isDirectionBlocked(BlockPos blockPos, Direction direction) {
        BlockPos adjacentPos = blockPos.relative(direction);
        BlockState state = MeteorClient.mc.level.getBlockState(adjacentPos);
        if (state.isAir()) {
            return false;
        }
        if (state.canOcclude() && state.isCollisionShapeFullBlock((net.minecraft.world.level.BlockGetter)MeteorClient.mc.level, adjacentPos)) {
            return true;
        }
        BlockEntity blockEntity = MeteorClient.mc.level.getBlockEntity(adjacentPos);
        return blockEntity instanceof Container;
    }

    private Vec3 getFaceMarkPos(BlockPos blockPos, Direction direction) {
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
        return new Vec3(x, y, z);
    }

    /**
     * Mirror of 19.5 bj.i(BlockPos): a position is valid for a mark when its
     * chunk is not loaded (keep the cached entry), or when the block entity
     * at the position is an inventory.
     */
    private boolean isValidContainerPos(BlockPos pos) {
        if (!MeteorClient.mc.level.isLoaded(pos)) {
            return true;
        } else {
            BlockEntity blockEntity = MeteorClient.mc.level.getBlockEntity(pos);
            return blockEntity instanceof Container;
        }
    }

    /** Clears everything (marks, render states, tracked positions). */
    public void clearAll() {
        this.markMap.clear();
        this.doubleMarkMap.clear();
        this.itemRenderStateMap.clear();
        this.reset();
    }

    // ---------------------------------------------------------------
    // On-disk cache (port of 19.5 aP, NBT format kept identical).
    // ---------------------------------------------------------------

    private static void save(String serverId, String worldId, Map<BlockPos, StorageWarp> markMap, Map<DoublePos, StorageWarp> doubleMarkMap, HolderLookup.Provider lookup) {
        if (serverId != null && !serverId.isBlank() && worldId != null && !worldId.isBlank()) {
            try {
                RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, lookup);
                CompoundTag root = new CompoundTag();
                ListTag containers = new ListTag();
                for (Map.Entry<BlockPos, StorageWarp> entry : markMap.entrySet()) {
                    CompoundTag tag = writeStorageItem(entry.getValue(), ops);
                    tag.putInt("x", entry.getKey().getX());
                    tag.putInt("y", entry.getKey().getY());
                    tag.putInt("z", entry.getKey().getZ());
                    containers.add(tag);
                }
                root.put("containers", containers);
                ListTag doubleContainers = new ListTag();
                for (Map.Entry<DoublePos, StorageWarp> entry : doubleMarkMap.entrySet()) {
                    CompoundTag tag = writeStorageItem(entry.getValue(), ops);
                    tag.putInt("x1", entry.getKey().getPos1().getX());
                    tag.putInt("y1", entry.getKey().getPos1().getY());
                    tag.putInt("z1", entry.getKey().getPos1().getZ());
                    tag.putInt("x2", entry.getKey().getPos2().getX());
                    tag.putInt("y2", entry.getKey().getPos2().getY());
                    tag.putInt("z2", entry.getKey().getPos2().getZ());
                    doubleContainers.add(tag);
                }
                root.put("doubleContainers", doubleContainers);
                Path path = cachePath(serverId, worldId);
                Files.createDirectories(path.getParent());
                NbtIo.writeCompressed(root, path);
            } catch (IOException e) {
                log.error("保存容器标识缓存失败", e);
            }
        }
    }

    private static void load(String serverId, String worldId, Map<BlockPos, StorageWarp> markMap, Map<DoublePos, StorageWarp> doubleMarkMap, HolderLookup.Provider lookup) {
        if (serverId != null && !serverId.isBlank() && worldId != null && !worldId.isBlank()) {
            try {
                Path path = cachePath(serverId, worldId);
                if (!Files.exists(path)) {
                    return;
                }
                CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, lookup);
                for (Tag element : root.getList("containers").orElseGet(ListTag::new)) {
                    if (element instanceof CompoundTag tag) {
                        BlockPos pos = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
                        markMap.put(pos, readStorageItem(tag, ops, 27));
                    }
                }
                for (Tag element : root.getList("doubleContainers").orElseGet(ListTag::new)) {
                    if (element instanceof CompoundTag tag) {
                        BlockPos pos1 = new BlockPos(tag.getIntOr("x1", 0), tag.getIntOr("y1", 0), tag.getIntOr("z1", 0));
                        BlockPos pos2 = new BlockPos(tag.getIntOr("x2", 0), tag.getIntOr("y2", 0), tag.getIntOr("z2", 0));
                        doubleMarkMap.put(new DoublePos(pos1, pos2), readStorageItem(tag, ops, 54));
                    }
                }
            } catch (IOException e) {
                log.error("加载容器标识缓存失败", e);
            }
        }
    }

    private static void delete(String serverId, String worldId) {
        if (serverId != null && !serverId.isBlank() && worldId != null && !worldId.isBlank()) {
            try {
                Files.deleteIfExists(cachePath(serverId, worldId));
            } catch (IOException e) {
                log.error("删除容器标识缓存失败", e);
            }
        }
    }

    private static CompoundTag writeStorageItem(StorageWarp storageWarp, RegistryOps<Tag> ops) {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        for (int i = 0; i < storageWarp.getSize(); ++i) {
            ItemStack stack = storageWarp.getStack(i);
            CompoundTag itemTag = new CompoundTag();
            if (stack != null && !stack.isEmpty()) {
                ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().ifPresent(encoded -> itemTag.put("stack", encoded));
            }
            items.add(itemTag);
        }
        tag.put("items", items);
        return tag;
    }

    private static StorageWarp readStorageItem(CompoundTag tag, RegistryOps<Tag> ops, int size) {
        ItemStack[] stacks = new ItemStack[size];
        Arrays.fill(stacks, ItemStack.EMPTY);
        ListTag items = tag.getList("items").orElseGet(ListTag::new);
        int count = Math.min(items.size(), size);
        for (int i = 0; i < count; ++i) {
            Tag element = items.get(i);
            if (element instanceof CompoundTag itemTag) {
                stacks[i] = ItemStack.OPTIONAL_CODEC.parse(ops, itemTag.getCompoundOrEmpty("stack")).result().orElse(ItemStack.EMPTY);
            }
        }
        return new StorageWarp(stacks);
    }

    private static Path cachePath(String serverId, String worldId) {
        return Const.LOTUS_DIR.resolve("preview").resolve(sanitize(serverId)).resolve(sanitize(worldId) + ".dat");
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}