package com.projetok.multiplayer;

import com.projetok.prototype.rpg.CosmeticCatalog;
import java.text.Normalizer;
import java.util.Objects;

/** Escolha de personagem da sessao experimental, sem conta ou identidade persistente. */
public record SessionCharacterSelection(String name,String race,String hair,String clothing) {
    public SessionCharacterSelection {
        name=Normalizer.normalize(Objects.requireNonNull(name,"nome").strip(),Normalizer.Form.NFC);
        new Protocol.Hello(Protocol.CHARACTER_APPLICATION,name);
        new Protocol.CharacterCreate(race,hair,clothing);
    }

    /** Retorna apenas os IDs cosmeticos; o servidor valida a disponibilidade no catalogo instalado. */
    public CosmeticCatalog.Selection cosmetics(){return new CosmeticCatalog.Selection(hair,clothing);}
}
