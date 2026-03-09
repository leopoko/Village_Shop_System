package com.github.leopoko.village_shop_system.villager;

/**
 * Duck interface for accessing VillagerShopBehavior on Villager entities.
 * Implemented by VillagerMixin.
 */
public interface ShopBehaviorAccessor {
    VillagerShopBehavior village_shop_system$getShopBehavior();

    /**
     * Expose the protected updateTrades() method for level-up trade refresh.
     */
    void village_shop_system$updateTrades();
}
