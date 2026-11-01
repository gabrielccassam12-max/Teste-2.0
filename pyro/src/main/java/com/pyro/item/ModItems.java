package com.pyro.item;

import com.pyro.Pyro;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
    private ModItems() {}

    public static final Item EMBER = Registry.register(Registries.ITEM, Pyro.id("ember_awakening"),
            new EmberItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(EMBER));
    }
}
