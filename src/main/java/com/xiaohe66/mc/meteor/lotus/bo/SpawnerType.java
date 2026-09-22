package com.xiaohe66.mc.meteor.lotus.bo;

import java.util.Set;
import net.minecraft.entity.EntityType;

public enum SpawnerType {
    DUNGEON("地牢", new OffsetRegion(-2, -1, -2, 1, 2, 1), EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_HORSE, EntityType.DROWNED, EntityType.SKELETON, EntityType.WITHER_SKELETON, EntityType.SKELETON_HORSE, EntityType.SPIDER),
    ABANDONED_MINESHAFT("废弃矿井", new OffsetRegion(-1, 0, -1, 1, 1, 1), EntityType.CAVE_SPIDER),
    STRONGHOLD("要塞", new OffsetRegion(-3, -2, -3, 3, 3, 3), EntityType.SILVERFISH),
    NETHER_FORTRESS("下界要塞", null, EntityType.BLAZE),
    BASTION("堡垒遗迹", null, EntityType.MAGMA_CUBE),
    SPAWNER("刷怪笼", null);

    private final String displayName;
    private final OffsetRegion region;
    private final Set<EntityType<?>> entityTypes;

    private SpawnerType(String displayName, OffsetRegion region, EntityType<?>... entityTypes) {
        this.displayName = displayName;
        this.region = region;
        this.entityTypes = Set.of(entityTypes);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public OffsetRegion getRegion() {
        return this.region;
    }

    public Set<EntityType<?>> getEntityTypes() {
        return this.entityTypes;
    }
}
