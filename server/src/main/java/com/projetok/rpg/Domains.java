package com.projetok.rpg;

import java.util.List;

/** Catalogo de caminhos e previews. As tecnicas abaixo ainda nao sao skills executaveis. */
public final class Domains {
    public record Definition(String id, String race, String name, String description,
                             List<String> techniquePreviews) {
        public Definition { techniquePreviews = List.copyOf(techniquePreviews); }
    }
    private static final List<Definition> VALUES = List.of(
        new Definition("water", "reaper", "Agua", "Fluidez, aparos e contra-ataques.",
            List.of("Aparo fluido: recebe o golpe e prepara uma resposta.", "Corte de mare: corte de espada com fluxo de agua.")),
        new Definition("thunder", "reaper", "Trovao", "Investidas e golpes rapidos.",
            List.of("Avanco eletrico: aproximacao veloz.", "Corte relampago: golpe rapido com abertura apos errar.")),
        new Definition("fire", "reaper", "Fogo", "Pressao, queimadura e controle de espaco.",
            List.of("Corte ardente: pressiona o alvo com fogo.", "Rastro de brasas: ocupa uma faixa do terreno.")),
        new Definition("wind", "reaper", "Vento", "Alcance de cortes e reposicionamento.",
            List.of("Corte de ar: projeta o alcance da espada.", "Passo de brisa: reposiciona o espadachim.")),
        new Definition("light", "human", "Luz", "Ofensiva, protecao e cura; pode ferir qualquer raca.",
            List.of("Pulso luminoso: atinge um adversario de qualquer origem.", "Cura canalizada: recuperacao com tempo de execucao.", "Barreira de luz: protecao espiritual.")),
        new Definition("martial", "human", "Marcial", "Evolucao avancada do combate desarmado.",
            List.of("Sequencia corporal: combina golpes proximos.", "Interrupcao marcial: explora uma abertura do adversario.")),
        new Definition("bow_long", "aetheri", "Arco de longa distancia", "Precisao, tiros carregados e manutencao de distancia.",
            List.of("Disparo concentrado: prepara uma flecha precisa.", "Tiro distante: alcanca alvos afastados.")),
        new Definition("bow_short", "aetheri", "Arco de curta distancia", "Disparos rapidos e mobilidade sob pressao.",
            List.of("Rajada proxima: dispara em sequencia.", "Leque de flechas: cobre uma abertura proxima.")),
        new Definition("darkness_debuff", "spirit", "Trevas: efeitos negativos", "Marcas, enfraquecimento e dano ao longo do tempo.",
            List.of("Marca sombria: prepara efeitos negativos.", "Desgaste espiritual: enfraquece o alvo ao longo do tempo.")),
        new Definition("darkness_control", "spirit", "Trevas: controle", "Lentidao, imobilizacoes breves e deslocamento. Pode atingir aliados, inclusive da party.",
            List.of("Zona sombria: reduz o movimento dentro da area.", "Prisão espiritual: restringe movimento por um periodo breve. Aliados tambem podem ser atingidos."))
    );
    private Domains() { }
    public static List<Definition> all() { return VALUES; }
    public static List<Definition> forRace(String race) {
        CharacterRaces.get(race);
        return VALUES.stream().filter(d -> d.race().equals(race)).toList();
    }
    public static Definition get(String id) {
        return VALUES.stream().filter(d -> d.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Dominio desconhecido: " + id));
    }
}
