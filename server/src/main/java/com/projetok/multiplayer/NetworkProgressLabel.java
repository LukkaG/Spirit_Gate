package com.projetok.multiplayer;

import javax.swing.JLabel;

/** Progressão recebida da sessão; ganhos são notificações, nunca XP aplicado no cliente. */
public final class NetworkProgressLabel extends JLabel {
    private GameplayProtocol.Progress previous;
    public void refresh(GameplayProtocol.Sheet sheet) {
        if(sheet==null){setText("Progressão: aguardando servidor");return;}
        var p=sheet.progress();
        String gain=previous!=null&&p.xp()>previous.xp()?" | +"+(p.xp()-previous.xp())+" XP"+(p.level()>previous.level()?" — Subiu de nível!":""):"";
        setText("Nível "+p.level()+" | XP "+p.xp()+" / "+p.nextLevelXp()+" | ATQ "+p.attack()+" DEF "+p.defense()+gain);
        setToolTipText("Domínio: "+p.domain()+" | Iniciação: nível 15 | Shunpo: nível 20 | Progresso desta sessão");
        previous=p;
    }
}
