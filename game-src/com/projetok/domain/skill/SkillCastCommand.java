package com.projetok.domain.skill;

/** Intenção imutável; valores recebidos são validados na execução pela simulação. */
public record SkillCastCommand(String skillId, float targetX, float targetY) { }
