/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.model;

import java.awt.Color;
import java.util.Random;

/**
 *
 * @author gitir
 */
public class Veiculo extends Thread {
    private int linha;
    private int coluna;
    private final Malha malha;
    private final int velocidade;
    private final Color cor;
    private volatile boolean ativo = true;

    public Veiculo(Malha malha, int linhaInicial, int colunaInicial, int velocidade) {
        this.malha = malha;
        this.linha = linhaInicial;
        this.coluna = colunaInicial;
        this.velocidade = velocidade;
        
        Random rand = new Random();
        this.cor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    @Override
    public void run() {
        while (ativo) {
            mover();

            try {
                Thread.sleep(velocidade);
            } catch (InterruptedException e) {
                ativo = false;
            }
        }
    }

    private void mover() {
        // Exemplo: apenas move para a direita
        if (coluna + 1 < malha.getColunas()) {
            coluna++;
        } else {
            ativo = false;
        }
    }

    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }
    public Color getCor() { return cor; }
    public boolean isAtivo() { return ativo; }
    
    
}
