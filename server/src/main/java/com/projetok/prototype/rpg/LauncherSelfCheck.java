package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Verificacao do pacote sem abrir janelas e sem tocar em personagens pessoais. */
final class LauncherSelfCheck {
    private LauncherSelfCheck() { }
    static void write(Path destination) {
        try {
            int races=0;
            for(var race:CharacterRaces.all()) {
                var world=new RpgWorld(CharacterIdentity.create("Verificacao",race.id(),"azure"));
                if(world.domainProgression().chosen()||world.castAbility()||world.castSecondaryAbility())
                    throw new IllegalStateException("Inicio de personagem invalido");
                var state=world.save();
                if(!RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(state))).save().equals(state))
                    throw new IllegalStateException("Falha no ciclo de save");
                if(Domains.forRace(race.id()).size()<2)throw new IllegalStateException("Caminhos ausentes");
                races++;
            }
            Path output=destination.toAbsolutePath();Files.createDirectories(output.getParent());
            Files.writeString(output,"{\"ok\":true,\"races\":"+races+",\"domains\":"+Domains.all().size()
                +",\"saveSchema\":2,\"paymentEnabled\":false}",StandardCharsets.UTF_8);
        } catch(Exception error) { throw new IllegalStateException("Verificacao do launcher falhou",error); }
    }
}
