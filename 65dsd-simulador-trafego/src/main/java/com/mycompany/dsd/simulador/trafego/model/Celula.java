package com.mycompany.dsd.simulador.trafego.model;

import com.mycompany.dsd.simulador.trafego.controller.ControleCelula;

public class Celula {
    private TipoCelula tipo;
    private final ControleCelula controle;

    public Celula(TipoCelula tipo, ControleCelula controle) {
        this.tipo = tipo;
        this.controle = controle;
    }

    public TipoCelula getTipo() {
        return tipo;
    }

    public void entrar() throws InterruptedException {
        controle.entrar();
    }
    
    public boolean tentarEntrar(long timeoutMs) throws InterruptedException {
        return controle.tentarEntrar(timeoutMs);
    }

    public void sair() {
        controle.sair();
    }

    public boolean isOcupada() {
        return controle.isOcupada();
    }
    
}
