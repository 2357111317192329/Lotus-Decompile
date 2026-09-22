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
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
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
    private final Map<ItemBo, ItemRenderState> itemRenderStateMap = new HashMap<>();
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
        if (this.persistCache.get() && MeteorClient.mc.world != null) {
            if (!this.markMap.isEmpty() || !this.doubleMarkMap.isEmpty()) {
                save(LotusUtils.getServerId(), LotusUtils.getWorldId(), this.markMap, this.doubleMarkMap, MeteorClient.mc.world.getRegistryManager());
            }
        }
    }

    /** Loads the mark maps from disk (persistable cache). */
    public void loadCache() {
        if (this.persistCache.get() && MeteorClient.mc.world != null) {
            load(LotusUtils.getServerId(), LotusUtils.getWorldId(), this.markMap, this.doubleMarkMap, MeteorClient.mc.world.getRegistryManager());
        }
    }

    /** Deletes the on-disk cache. */
    public void deleteCache() {
        delete(LotusUtils.getServerId(), LotusUtils.getWorldId());
    }

    public void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        if (this.isValidContainerPos(pos)) {
            this.lastInteractPos = pos.toImmutable();
            this.lastInteractTime = MeteorClient.mc.world == null ? -1L : MeteorClient.mc.world.getTime();
        } else {
            this.lastInteractPos = null;
        }
    }

    public void onOpenScreen(OpenScreenEvent event) {
        Screen screen = event.screen;
        if (screen instanceof HandledScreen handledScreen) {
            if (handledScreen.getScreenHandler() instanceof PlayerScreenHandler || handledScreen.getScreenHandler() instanceof CreativeInventoryScreen.CreativeScreenHandler) {
                this.reset();
                return;
            }
            BlockPos pos = this.findContainerPos();
            if (pos != null) {
                this.currentContainerPos = pos.toImmutable();
                this.lastSyncId = -1;
            }
        }
    }

    private BlockPos findContainerPos() {
        if (this.lastInteractPos != null && MeteorClient.mc.world != null && MeteorClient.mc.world.getTime() - this.lastInteractTime <= 5L && this.isValidContainerPos(this.lastInteractPos)) {
            return this.lastInteractPos;
        } else {
            HitResult hitResult = MeteorClient.mc.crosshairTarget;
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
        if (!(MeteorClient.mc.currentScreen instanceof HandledScreen)) {
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
    public void update(ScreenHandler menu) {
        if (this.currentContainerPos != null) {
            if (menu.syncId != this.lastSyncId || menu.getRevision() != this.lastRevision) {
                this.lastSyncId = menu.syncId;
                this.lastRevision = menu.getRevision();
                int size = HeInvUtils.getScreenMainSize();
                ItemStack[] stacks = new ItemStack[size];
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = menu.getSlot(i).getStack();
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
        Vec3d cameraPos = event.getPos();
        this.markMap.entrySet().removeIf(entry -> !this.isValidContainerPos(entry.getKey()));
        for (Map.Entry<BlockPos, StorageWarp> entry : this.markMap.entrySet()) {
            BlockPos blockPos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = this.findBestDirection(blockPos, cameraPos);
            this.renderMark(event, blockPos, clickDirection, cameraPos, storageWarp);
        }
    }

    private void renderDoubleMarks(WorldEntityRenderEvent event) {
        Vec3d cameraPos = event.getPos();
        this.doubleMarkMap.entrySet().removeIf(entry -> !this.isValidContainerPos(entry.getKey().getPos1()) || !this.isValidContainerPos(entry.getKey().getPos2()));
        for (Map.Entry<DoublePos, StorageWarp> entry : this.doubleMarkMap.entrySet()) {
            DoublePos doublePos = entry.getKey();
            StorageWarp storageWarp = entry.getValue();
            Direction clickDirection = null;
            BlockPos markBlockPos = null;
            double bestScore = -1.7976931348623157E308;
            for (BlockPos cornerPos : Arrays.asList(doublePos.getPos1(), doublePos.getPos2())) {
                Vec3d cornerCenter = new Vec3d((double)cornerPos.getX() + 0.5, (double)cornerPos.getY() + 0.5, (double)cornerPos.getZ() + 0.5);
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

    private void renderMark(WorldEntityRenderEvent event, BlockPos blockPos, Direction clickDirection, Vec3d cameraPos, StorageWarp storageWarp) {
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
        int light = WorldRenderer.getLightmapCoordinates((BlockRenderView)MeteorClient.mc.world, blockPos.offset(clickDirection));
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
            MeteorClient.mc.getItemModelManager().clearAndUpdate(renderState, stack, ItemDisplayContext.FIXED, (World)MeteorClient.mc.world, null, 0);
            return renderState;
        });
    }

    private Direction findBestDirection(BlockPos blockPos, Vec3d cameraPos) {
        Direction direction;
        Vec3d centerPos = new Vec3d((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5);
        List sortedDirections = Arrays.stream(Direction.values()).sorted((dir1, dir2) -> Double.compare(this.getDirectionScore(centerPos, cameraPos, (Direction)dir2), this.getDirectionScore(centerPos, cameraPos, (Direction)dir1))).toList();
        java.util.Iterator iterator = sortedDirections.iterator();
        while (iterator.hasNext() && !(this.getDirectionScore(centerPos, cameraPos, direction = (Direction)iterator.next()) <= 0.0)) {
            if (this.isDirectionBlocked(blockPos, direction)) continue;
            return direction;
        }
        return (Direction)sortedDirections.getFirst();
    }

    private double getDirectionScore(Vec3d centerPos, Vec3d cameraPos, Direction direction) {
        Vec3d faceCenter = this.getFaceCenter(centerPos, direction);
        Vec3d offset = cameraPos.subtract(faceCenter);
        Vec3i directionVec = direction.getVector();
        return offset.x * (double)directionVec.getX() + offset.y * (double)directionVec.getY() + offset.z * (double)directionVec.getZ();
    }

    private Vec3d getFaceCenter(Vec3d pos, Direction direction) {
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
        BlockPos adjacentPos = blockPos.offset(direction);
        BlockState state = MeteorClient.mc.world.getBlockState(adjacentPos);
        if (state.isAir()) {
            return false;
        }
        if (state.isOpaque() && state.isFullCube((net.minecraft.world.BlockView)MeteorClient.mc.world, adjacentPos)) {
            return true;
        }
        BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(adjacentPos);
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

    /**
     * Mirror of 19.5 bj.i(BlockPos): a position is valid for a mark when its
     * chunk is not loaded (keep the cached entry), or when the block entity
     * at the position is an inventory.
     */
    private boolean isValidContainerPos(BlockPos pos) {
        if (!MeteorClient.mc.world.isPosLoaded(pos)) {
            return true;
        } else {
            BlockEntity blockEntity = MeteorClient.mc.world.getBlockEntity(pos);
            return blockEntity instanceof Inventory;
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

    private static void save(String serverId, String worldId, Map<BlockPos, StorageWarp> markMap, Map<DoublePos, StorageWarp> doubleMarkMap, RegistryWrapper.WrapperLookup lookup) {
        if (serverId != null && !serverId.isBlank() && worldId != null && !worldId.isBlank()) {
            try {
                RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
                NbtCompound root = new NbtCompound();
                NbtList containers = new NbtList();
                for (Map.Entry<BlockPos, StorageWarp> entry : markMap.entrySet()) {
                    NbtCompound tag = writeStorageItem(entry.getValue(), ops);
                    tag.putInt("x", entry.getKey().getX());
                    tag.putInt("y", entry.getKey().getY());
                    tag.putInt("z", entry.getKey().getZ());
                    containers.add(tag);
                }
                root.put("containers", containers);
                NbtList doubleContainers = new NbtList();
                for (Map.Entry<DoublePos, StorageWarp> entry : doubleMarkMap.entrySet()) {
                    NbtCompound tag = writeStorageItem(entry.getValue(), ops);
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

    private static void load(String serverId, String worldId, Map<BlockPos, StorageWarp> markMap, Map<DoublePos, StorageWarp> doubleMarkMap, RegistryWrapper.WrapperLookup lookup) {
        if (serverId != null && !serverId.isBlank() && worldId != null && !worldId.isBlank()) {
            try {
                Path path = cachePath(serverId, worldId);
                if (!Files.exists(path)) {
                    return;
                }
                NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
                for (NbtElement element : root.getList("containers").orElseGet(NbtList::new)) {
                    if (element instanceof NbtCompound tag) {
                        BlockPos pos = new BlockPos(tag.getInt("x", 0), tag.getInt("y", 0), tag.getInt("z", 0));
                        markMap.put(pos, readStorageItem(tag, ops, 27));
                    }
                }
                for (NbtElement element : root.getList("doubleContainers").orElseGet(NbtList::new)) {
                    if (element instanceof NbtCompound tag) {
                        BlockPos pos1 = new BlockPos(tag.getInt("x1", 0), tag.getInt("y1", 0), tag.getInt("z1", 0));
                        BlockPos pos2 = new BlockPos(tag.getInt("x2", 0), tag.getInt("y2", 0), tag.getInt("z2", 0));
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

    private static NbtCompound writeStorageItem(StorageWarp storageWarp, RegistryOps<NbtElement> ops) {
        NbtCompound tag = new NbtCompound();
        NbtList items = new NbtList();
        for (int i = 0; i < storageWarp.getSize(); ++i) {
            ItemStack stack = storageWarp.getStack(i);
            NbtCompound itemTag = new NbtCompound();
            if (stack != null && !stack.isEmpty()) {
                ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().ifPresent(encoded -> itemTag.put("stack", encoded));
            }
            items.add(itemTag);
        }
        tag.put("items", items);
        return tag;
    }

    private static StorageWarp readStorageItem(NbtCompound tag, RegistryOps<NbtElement> ops, int size) {
        ItemStack[] stacks = new ItemStack[size];
        Arrays.fill(stacks, ItemStack.EMPTY);
        NbtList items = tag.getList("items").orElseGet(NbtList::new);
        int count = Math.min(items.size(), size);
        for (int i = 0; i < count; ++i) {
            NbtElement element = items.get(i);
            if (element instanceof NbtCompound itemTag) {
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