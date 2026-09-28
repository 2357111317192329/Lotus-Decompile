package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.Set;

public enum SpawnerType {
    DUNGEON("地牢", new OffsetRegion(-2, -1, -2, 1, 2, 1), EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.ZOMBIE_HORSE, EntityTypes.ZOMBIE_NAUTILUS, EntityTypes.SKELETON, EntityTypes.WITHER_SKELETON, EntityTypes.SKELETON_HORSE, EntityTypes.SPIDER),
    ABANDONED_MINESHAFT("废弃矿井", new OffsetRegion(-1, 0, -1, 1, 1, 1), EntityTypes.CAVE_SPIDER),
    STRONGHOLD("要塞", new OffsetRegion(-3, -2, -3, 3, 3, 3), EntityTypes.SILVERFISH),
    NETHER_FORTRESS("下界要塞", null, EntityTypes.BLAZE),
    BASTION("堡垒遗迹", null, EntityTypes.MAGMA_CUBE),
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
