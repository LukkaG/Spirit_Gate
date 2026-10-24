package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import java.util.*;

/** Somente associações explicitamente aprovadas; nunca infere arte a partir do nome do item. */
public final class EquipmentAppearanceAdapter {
    private final Map<String,CosmeticCatalog.Selection> approved;
    public EquipmentAppearanceAdapter(CosmeticCatalog catalog,Map<String,CosmeticCatalog.Selection> approved){
        var items=new ItemCatalog(RpgContent.builtin());
        approved.forEach((id,selection)->{if(items.get(id).category()!=ItemCatalog.Category.EQUIPMENT)throw new IllegalArgumentException("Item sem equipamento");catalog.validate(selection);});
        this.approved=Map.copyOf(approved);
    }
    public Optional<CosmeticCatalog.Selection> appearance(String equipmentId){return Optional.ofNullable(approved.get(equipmentId));}
}
