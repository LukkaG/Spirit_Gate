package com.projetok.prototype;

import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.LightningStepService;
import com.projetok.prototype.DebugSnapshot.SkillFeedbackKind;
import com.projetok.prototype.DebugSnapshot.SkillFeedbackView;
import com.projetok.prototype.DebugSnapshot.SkillHudView;
import com.projetok.prototype.DebugSnapshot.SkillState;

import java.util.Locale;

/**
 * Converte valores autoritativos em descrições imutáveis para o HUD.
 * Não consulta componentes mutáveis, não consome recursos e não executa habilidades.
 */
public final class SkillHudMapper {
    private static final SkillFeedbackView NONE = new SkillFeedbackView(SkillFeedbackKind.NONE, "", 0f);

    private SkillHudMapper() { }

    // --- Disponibilidade atual, independente do retorno temporário da última tentativa ---

    /** Descreve a disponibilidade atual da Criostase sem alterar o serviço. */
    public static SkillHudView cryostasis(boolean dead, float stamina, float cooldown,
                                          SkillFeedbackView feedback) {
        return map(CryostasisService.ID, "Q", "CRIOSTASE", CryostasisService.COST,
                dead, stamina, cooldown, false, false, feedback);
    }

    /** Distingue deslocamento ativo, espera por saída livre, recarga e falta de vigor. */
    public static SkillHudView lightningStep(boolean dead, float stamina, float cooldown,
                                             boolean active, boolean awaitingLanding,
                                             SkillFeedbackView feedback) {
        return map(LightningStepService.ID, "SHIFT", "PASSO RELAMPAGO", LightningStepService.COST,
                dead, stamina, cooldown, active, awaitingLanding, feedback);
    }

    /** Aplica uma ordem visual explícita sem interferir na elegibilidade do domínio. */
    private static SkillHudView map(String id, String key, String label, float cost, boolean dead,
                                    float stamina, float cooldown, boolean active,
                                    boolean awaitingLanding, SkillFeedbackView feedback) {
        SkillState state = dead ? SkillState.DEAD : awaitingLanding ? SkillState.AWAITING_LANDING
                : active ? SkillState.ACTIVE : cooldown > 0f ? SkillState.COOLDOWN
                : stamina < cost ? SkillState.INSUFFICIENT_STAMINA : SkillState.READY;
        String stateText = switch (state) {
            case DEAD -> "JOGADOR DERROTADO";
            case AWAITING_LANDING -> "AGUARDANDO SAIDA LIVRE";
            case ACTIVE -> String.format(Locale.ROOT, "EM MOVIMENTO | RECARGA %.1f s", cooldown);
            case COOLDOWN -> String.format(Locale.ROOT, "RECARGA %.1f s", cooldown);
            // O saldo exato fica no view/barra; arredondá-lo aqui poderia exibir
            // 35/35 para um float ainda abaixo do custo e contradizer a recusa.
            case INSUFFICIENT_STAMINA -> "VIGOR INSUFICIENTE";
            case READY -> "DISPONIVEL";
        };
        return new SkillHudView(id, key, label, cost, cooldown, stamina, state, stateText, feedback);
    }

    // --- Retornos breves conservam o motivo real retornado pelo domínio ---

    /** Retorna um marcador compartilhado e imutável de ausência de mensagem. */
    public static SkillFeedbackView none() { return NONE; }

    /** Converte exatamente o resultado do lançamento da Criostase. */
    public static SkillFeedbackView feedback(CryostasisService.Status status, float duration) {
        return message(switch (status) {
            case CAST -> SkillFeedbackKind.ACCEPTED;
            case INVALID -> SkillFeedbackKind.INVALID_TARGET;
            case DEAD -> SkillFeedbackKind.DEAD;
            case COOLDOWN -> SkillFeedbackKind.COOLDOWN;
            case STAMINA -> SkillFeedbackKind.INSUFFICIENT_STAMINA;
        }, duration);
    }

    /** Converte exatamente o resultado da tentativa do Passo Relâmpago. */
    public static SkillFeedbackView feedback(LightningStepService.Status status, float duration) {
        return message(switch (status) {
            case STARTED -> SkillFeedbackKind.ACCEPTED;
            case INVALID -> SkillFeedbackKind.INVALID_TARGET;
            case DEAD -> SkillFeedbackKind.DEAD;
            case ZERO_DIRECTION -> SkillFeedbackKind.ZERO_DIRECTION;
            case COOLDOWN -> SkillFeedbackKind.COOLDOWN;
            case STAMINA -> SkillFeedbackKind.INSUFFICIENT_STAMINA;
            case LANDING_PENDING -> SkillFeedbackKind.AWAITING_LANDING;
        }, duration);
    }

    /** Publica contato confirmado pela varredura seletiva, sem supor a geometria pelo alvo. */
    public static SkillFeedbackView blockedFeedback(float duration) {
        return message(SkillFeedbackKind.SOLID_OR_BOUNDARY, duration);
    }

    /** Avança somente uma cópia do relógio visual; o mundo chama uma vez por tick. */
    public static SkillFeedbackView elapse(SkillFeedbackView feedback, float dt) {
        float remaining = Math.max(0f, feedback.remaining() - dt);
        return remaining > 0f
                ? new SkillFeedbackView(feedback.kind(), feedback.message(), remaining) : NONE;
    }

    /** Associa cada motivo a uma mensagem curta, calculada antes da renderização. */
    private static SkillFeedbackView message(SkillFeedbackKind kind, float duration) {
        String message = switch (kind) {
            case NONE -> "";
            case ACCEPTED -> "ATIVADA";
            case INVALID_TARGET -> "RECUSADA: ALVO INVALIDO";
            case DEAD -> "RECUSADA: JOGADOR DERROTADO";
            case ZERO_DIRECTION -> "RECUSADA: DIRECAO ZERO";
            case COOLDOWN -> "RECUSADA: EM RECARGA";
            case INSUFFICIENT_STAMINA -> "RECUSADA: VIGOR INSUFICIENTE";
            case AWAITING_LANDING -> "RECUSADA: AGUARDANDO SAIDA";
            case SOLID_OR_BOUNDARY -> "BLOQUEIO: SOLIDO / BORDA";
        };
        return new SkillFeedbackView(kind, message, duration);
    }
}
