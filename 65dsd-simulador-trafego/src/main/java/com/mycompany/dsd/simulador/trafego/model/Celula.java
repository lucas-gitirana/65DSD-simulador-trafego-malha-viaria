package com.mycompany.dsd.simulador.trafego.model;

public class Celula {
    private TipoCelula tipo;

    public Celula(TipoCelula tipo) {
        this.tipo = tipo;
    }

    public TipoCelula getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        return tipo.name();
    }
    
}
