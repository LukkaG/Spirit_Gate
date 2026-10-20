package com.projetok.studio;
/** Diagnostico navegavel com evidencia e sugestao, sem correcao automatica. */
public record StudioProblem(Severity severity,String map,String element,String description,String evidence,String correction) {
    /** Erros bloqueiam exportacao; avisos explicam limitacoes. */
    public enum Severity { ERROR, WARNING }
    @Override public String toString(){return severity+" | "+map+" / "+element+" | "+description+" | "+correction;}
}
