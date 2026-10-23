# Quest Builder experimental

Abra World Studio → Missões → Abrir Quest Builder. A lista à esquerda pesquisa IDs e nomes; o centro contém objetivos reordenáveis; o inspetor à direita configura texto, NPC, região, nível, raça, domínio, dependências e recompensa.

Use Aplicar edição, Validar e Salvar. Exportar para teste produz `.sgquest`; Testar no RPG abre um perfil sintético novo, com raça e nível escolhidos explicitamente. Selecionar domínio nessa preparação exige nível 15 e utiliza `RpgWorld.chooseDomain`. Shunpo continua exigindo nível 20.

O formato `.sgquest` V1 é um documento de autoria em Properties UTF-8, sem scripts. Campos desconhecidos, versões incompatíveis e chaves duplicadas são rejeitados. Limites: 1 MB, 64 missões, oito objetivos por missão, contagens 1..100 e recompensas conforme QuestService. Há uma recompensa de item por missão, além de XP e moedas do RPG. Moedas do RPG não representam compra de Spirit Coins.

Objetivos suportados: kill, collect, talk e visit. Referências vêm de RpgContent, CharacterRaces e Domains. A validação exige compatibilidade racial do domínio, nível mínimo 15 quando há requisito de domínio, dependências existentes e ausência de ciclos. Nenhum domínio ou habilidade é concedido por configuração visual.

Em playtest, aproxime-se do NPC inicial e use Aceitar no NPC. Execute objetivos com os controles reais do RPG e conclua pelo mesmo NPC. O ledger de autoria é mantido por AuthoredQuestSession, que delega eventos, contagem e conclusão ao QuestService real. RpgWorld aplica um plano de recompensa inteiramente validado antes de publicar inventário, moedas e XP. Conclusão repetida não recompensa novamente.

O save V2 continua restrito às quests de produção. O ledger autorado não entra nas flags ou quests do personagem e termina junto com a sessão. RpgRuntime recusa gravação de personagem em sessões sintéticas. Não há retomada persistente de quest autorada; uma evolução futura exige contrato de schema e autorização própria. Salvar o documento de autoria não salva o progresso do playtest.

Contrato de reset pode ser descrito e validado, mas completar uma missão com esse marcador não executa reset, não marca pagamento nem habilita carteira. Os testes sintéticos da política de reset não acionam contas reais.

Evidência reproduzível: compilar `tools/QuestDemoProbe.java` contra a saída atual e executar `com.projetok.studio.QuestDemoProbe <pasta-nova>`. O probe cria a missão pelo formulário, grava/reabre `.sgquest`, executa aceitação e conclusão pelo runtime e confirma rejeição da segunda recompensa. Resultado e captura são gravados somente na pasta de evidências.
