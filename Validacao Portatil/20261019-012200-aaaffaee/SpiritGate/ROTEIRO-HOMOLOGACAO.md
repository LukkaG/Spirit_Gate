# Spirit Gate 0.36.0-rc1 - homologação humana pendente

Nenhum item abaixo foi homologado em outro computador pelo agente.
Em outro Windows x64, sem Java/Maven: extrair ZIP inteiro em pasta gravável com
espaços, conferir SHA externo e VERSION.txt. Abrir SpiritGate.exe; fechar e reabrir.
Preservar app/ e runtime/. Executável sem assinatura. Sem rede, login ou banco.

- TASK-022: conferir versão/base e inventário da entrega.
- TASK-023: no ambiente de desenvolvimento, rodar python tools/qualidade.py --offline.
- TASK-024: abrir mapa opcional com SpiritGate.exe --map kit-homologacao/mapa-demo.properties;
  andar além do cenário histórico e usar R. Câmera deve acompanhar e R restaura spawn.
- TASK-025: observar objetos cruzando bordas da câmera, sem desaparecimento precoce.
- TASK-026: conferir tiles sintéticos atrás/à frente do personagem, com H mostrando
  que decoração não altera hitboxes. Arte final não está integrada.
- TASK-027: na fonte, criar mapa com tools/mapas.py e validar com o mesmo carregador.
- TASK-028: abrir docs/autoria-028-sintetica/revisao/preview.html no pacote de evidências;
  template vazio é apenas sintético. Importar exportação real do Aseprite para revisão.
- TASK-029: iniciar sem pacote visual: fallback deve funcionar. Não aprovar arte pelo
  resultado de fixtures. Revisar cada exportação real antes de staging.
- TASK-030: caminhar/atacar, receber dano, morrer, congelar mob e usar R durante ação.
- TASK-031: segurar WASD/E, alternar janela, voltar e testar nova pressão; verificar
  Q/Shift após resize e cursor no HUD. Shift mantido não repete sozinho após recarga.
- TASK-032: F1 abre/fecha ajuda; conferir HP, vigor, custos, recargas e estados.
- TASK-033: no mapa padrão, combater e observar impactos, Freeze e derrota, sem
  alteração dos valores. A chance de Freeze continua probabilística.
- TASK-034: F2 salva ZIP com nome novo. Abrir diagnostico.json, revisar e só então
  anexar manualmente ao relato. Nenhum envio é automático.
- TASK-035: jogar sessão prolongada, repetir R e habilidades; relatar travamentos
  com versão e passos. O estresse sintético não substitui este teste.
- TASK-036: conferir JVM integrada e abertura em outro Windows sem Java instalado.

Mapa opcional é sintético 1920x1280, spawn 1500,1000, sólido à direita e leve acima.
Use o mapa padrão para combate próximo aos mobs. Na fonte, o zoom 1-4 é API de
integração testada; nenhum atalho novo de zoom foi criado nesta rodada.
