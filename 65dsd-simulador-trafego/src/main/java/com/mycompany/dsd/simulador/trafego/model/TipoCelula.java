package com.mycompany.dsd.simulador.trafego.model;

public enum TipoCelula {
    VAZIO(0),
    ESTRADA_CIMA(1),
    ESTRADA_DIREITA(2),
    ESTRADA_BAIXO(3),
    ESTRADA_ESQUERDA(4),
    CRUZAMENTO_CIMA(5),
    CRUZAMENTO_DIREITA(6),
    CRUZAMENTO_BAIXO(7),
    CRUZAMENTO_ESQUERDA(8),
    CRUZAMENTO_CIMA_DIREITA(9),
    CRUZAMENTO_CIMA_ESQUERDA(10),
    CRUZAMENTO_DIREITA_BAIXO(11),
    CRUZAMENTO_BAIXO_ESQUERDA(12);
    
    private final int valor;
    
    TipoCelula(int valor) {
        this.valor = valor;
    }
    
    public int getValor() {
        return valor;
    }
    
    public static TipoCelula fromInt(int valor) {
        for (TipoCelula t : TipoCelula.values()) {
            if (t.valor == valor) {
                return t;
            }
        }
        return VAZIO;
    }
}
