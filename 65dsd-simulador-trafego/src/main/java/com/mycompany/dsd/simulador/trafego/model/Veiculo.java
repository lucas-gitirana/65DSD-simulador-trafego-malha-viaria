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
    private static int contadorVeiculos = 0;
    private final int id;
    private int linha;
    private int coluna;
    private final Malha malha;
    private final int velocidade;
    private final Color cor;
    private volatile boolean ativo = true;

    private Celula celulaAtual;

    public Veiculo(Malha malha, int linhaInicial, int colunaInicial, int velocidade) {
        this.malha = malha;
        this.linha = linhaInicial;
        this.coluna = colunaInicial;
        this.velocidade = velocidade;

        Random rand = new Random();
        this.cor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        this.id = ++contadorVeiculos;
    }

    public int getVeiculoId() {
        return id;
    }

    @Override
    public void run() {
        this.celulaAtual = malha.getCelula(linha, coluna);
        if (this.celulaAtual == null) {
            ativo = false;
            return;
        }

        try {
            this.celulaAtual.entrar();
            while (ativo) {
                Thread.sleep(velocidade);
                mover();
            }
        } catch (InterruptedException e) {
            ativo = false;
            Thread.currentThread().interrupt();
        } finally {
            if (this.celulaAtual != null && this.celulaAtual.isOcupada()) {
                this.celulaAtual.sair();
            }
        }
    }

    private void mover() throws InterruptedException {
        int proximaLinha = linha;
        int proximaColuna = coluna;

        TipoCelula tipoAtual = this.celulaAtual.getTipo();

        switch (tipoAtual) {
            case ESTRADA_DIREITA:
                proximaColuna++;
                break;
            case ESTRADA_ESQUERDA:
                proximaColuna--;
                break;
            case ESTRADA_CIMA:
                proximaLinha--;
                break;
            case ESTRADA_BAIXO:
                proximaLinha++;
                break;
            default:
                ativo = false;
                return;
        }

        if (proximaLinha < 0 || proximaLinha >= malha.getLinhas() ||
                proximaColuna < 0 || proximaColuna >= malha.getColunas()) {
            ativo = false;
            return;
        }

        Celula proximaCelula = malha.getCelula(proximaLinha, proximaColuna);

        if (proximaCelula == null || proximaCelula.getTipo() == TipoCelula.VAZIO) {
            ativo = false;
            return;
        }

        proximaCelula.entrar();

        this.linha = proximaLinha;
        this.coluna = proximaColuna;
        this.celulaAtual.sair();
        this.celulaAtual = proximaCelula;
    }

    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }
    public Color getCor() { return cor; }
    public boolean isAtivo() { return ativo; }
}