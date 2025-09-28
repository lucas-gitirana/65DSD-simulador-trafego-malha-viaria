/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.controller;

import com.mycompany.dsd.simulador.trafego.model.Malha;
import com.mycompany.dsd.simulador.trafego.model.Veiculo;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author gitir
 */
public class Simulacao extends Thread {
    private final Malha malha;
    private final List<Veiculo> veiculos;
    private final int intervaloInsercao;
    private final int maxVeiculos;
    private volatile boolean rodando = true;
    private volatile boolean inserindo = false;
    
    public Simulacao(Malha malha, int intervaloInsercao, int maxVeiculos) {
        this.malha = malha;
        this.veiculos = new CopyOnWriteArrayList<>();
        this.intervaloInsercao = intervaloInsercao;
        this.maxVeiculos = maxVeiculos;
    }
    
    public void iniciarInsercao() { inserindo = true; }
    public void pararInsercao() { inserindo = false; }
    public void encerrar() { rodando = false; }
    
    public List<Veiculo> getVeiculos() { return veiculos; }

    @Override
    public void run() {
        while (rodando) {
            if (inserindo && veiculos.size() < maxVeiculos) {
                Veiculo v = new Veiculo(malha, 0, 0, 500 + (int)(Math.random() * 500));
                veiculos.add(v);
                v.start();
            }

            try {
                Thread.sleep(intervaloInsercao);
            } catch (InterruptedException e) {
                rodando = false;
            }
        }
    }
}
