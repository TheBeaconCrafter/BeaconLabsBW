package org.bcnlab.beaconLabsBW.shop;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;

/**
 * Serializable data structure for storing shop villager information within an Arena config.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopVillagerData {
    @Expose
    private ShopVillager.VillagerType type;
    @Expose
    private SerializableLocation location;
} 