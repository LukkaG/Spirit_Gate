package com.projetok.domain.entity;

import java.util.List;

public record DialogueScript(String npcName, List<String> lines) {

    public DialogueScript {
        lines = List.copyOf(lines);
    }

    public static DialogueScript of(String npcName, String... lines) {
        return new DialogueScript(npcName, List.of(lines));
    }
}
