package org.hackedserver.spigot;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class HackedHolder implements InventoryHolder {

    private final Inventory inventory;

    public HackedHolder(HumanEntity player) {
        this.inventory = player.getOpenInventory().getTopInventory();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
