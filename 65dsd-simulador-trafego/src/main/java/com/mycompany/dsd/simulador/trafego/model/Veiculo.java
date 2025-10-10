/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private Point ultimaPosicao = null;
    
    private final Random rnd = new Random();
    private static final long TIMEOUT_POR_CELULA_MS = 300; //ajustar
    private static final int BACKOFF_MIN_MS = 100;
    private static final int BACKOFF_MAX_MS = 300;

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

//    private void mover() throws InterruptedException {
//        int proximaLinha = linha;
//        int proximaColuna = coluna;
//
//        TipoCelula tipoAtual = this.celulaAtual.getTipo();
//
//        switch (tipoAtual) {
//            case ESTRADA_DIREITA:
//                proximaColuna++;
//                break;
//            case ESTRADA_ESQUERDA:
//                proximaColuna--;
//                break;
//            case ESTRADA_CIMA:
//                proximaLinha--;
//                break;
//            case ESTRADA_BAIXO:
//                proximaLinha++;
//                break;
//            default:
//                ativo = false;
//                return;
//        }
//
//        if (proximaLinha < 0 || proximaLinha >= malha.getLinhas() ||
//                proximaColuna < 0 || proximaColuna >= malha.getColunas()) {
//            ativo = false;
//            return;
//        }
//
//        Celula proximaCelula = malha.getCelula(proximaLinha, proximaColuna);
//
//        if (proximaCelula == null || proximaCelula.getTipo() == TipoCelula.VAZIO) {
//            ativo = false;
//            return;
//        }
//
//        proximaCelula.entrar();
//
//        this.linha = proximaLinha;
//        this.coluna = proximaColuna;
//        this.celulaAtual.sair();
//        this.celulaAtual = proximaCelula;
//    }
    
    
    private void mover() throws InterruptedException {
        int proximaLinha = linha;
        int proximaColuna = coluna;

        TipoCelula tipoAtual = this.celulaAtual.getTipo();

        if (malha.isCruzamento(tipoAtual)) {
            // Tentativa de múltiplos tries antes de desistir
            final int tentativasMax = 5;
            Point destino = null;
            for (int tent = 0; tent < tentativasMax && destino == null; tent++) {
                destino = decidirProximoPassoCruzamento(tipoAtual);
                if (destino == null) {
                    // Espera um pouco antes de tentar novamente
                    Thread.sleep(BACKOFF_MIN_MS + rnd.nextInt(BACKOFF_MAX_MS - BACKOFF_MIN_MS));
                }
            }
            if (destino == null) {
                System.out.println("Veículo morreu no cruzamento (" + linha + "," + coluna + ") - sem saída livre.");
                ativo = false;
                return;
            }
            proximaLinha = destino.y;
            proximaColuna = destino.x;
        } else {
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
                    System.out.println("Veículo morreu em (" + linha + "," + coluna + ") - tipo desconhecido.");
                    ativo = false;
                    return;
            }
        }

        // Checagem de limites
        if (proximaLinha < 0 || proximaLinha >= malha.getLinhas() ||
                proximaColuna < 0 || proximaColuna >= malha.getColunas()) {
            System.out.println("Veículo morreu ao sair da malha (" + proximaLinha + "," + proximaColuna + ").");
            ativo = false;
            return;
        }

        Celula proximaCelula = malha.getCelula(proximaLinha, proximaColuna);

        // Evitar desaparecer em células vazias
        if (proximaCelula == null || proximaCelula.getTipo() == TipoCelula.VAZIO) {
            System.out.println("Veículo morreu em célula vazia (" + proximaLinha + "," + proximaColuna + ").");
            ativo = false;
            return;
        }

        // Usa tentarEntrar para evitar deadlock
        boolean entrou = proximaCelula.tentarEntrar(TIMEOUT_POR_CELULA_MS);
        if (!entrou) {
            // Faz backoff pequeno e tenta novamente na próxima iteração do loop principal
            Thread.sleep(BACKOFF_MIN_MS + rnd.nextInt(BACKOFF_MAX_MS - BACKOFF_MIN_MS));
            return;
        }

        this.linha = proximaLinha;
        this.coluna = proximaColuna;
        this.celulaAtual.sair();
        ultimaPosicao = new Point(coluna, linha);
        this.celulaAtual = proximaCelula;
    }
    
    private Point decidirProximoPassoCruzamento(TipoCelula tipo) {
        List<Point> opcoes = new ArrayList<>();

        // 🔹 Pega direções válidas a partir do tipo de cruzamento
        switch (tipo) {
            case CRUZAMENTO_CIMA:
            case CRUZAMENTO_CIMA_DIREITA:
            case CRUZAMENTO_CIMA_ESQUERDA:
                opcoes.add(new Point(coluna, linha - 1)); // cima
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_BAIXO:
            case CRUZAMENTO_BAIXO_ESQUERDA:
            case CRUZAMENTO_DIREITA_BAIXO:
                opcoes.add(new Point(coluna, linha + 1)); // baixo
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_DIREITA:
            case CRUZAMENTO_CIMA_DIREITA:
            case CRUZAMENTO_DIREITA_BAIXO:
                opcoes.add(new Point(coluna + 1, linha)); // direita
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_ESQUERDA:
            case CRUZAMENTO_CIMA_ESQUERDA:
            case CRUZAMENTO_BAIXO_ESQUERDA:
                opcoes.add(new Point(coluna - 1, linha)); // esquerda
                break;
        }

        // 🔹 Filtra opções válidas (dentro da malha e não vazias)
        List<Point> validas = new ArrayList<>();
        for (Point p : opcoes) {
            Celula c = malha.getCelula(p.y, p.x);
            if (c != null && c.getTipo() != TipoCelula.VAZIO && !c.isOcupada()) {
                validas.add(p);
            }
        }

        if (validas.isEmpty()) return null;
        
        if (ultimaPosicao != null) {
            validas.removeIf(p -> p.equals(ultimaPosicao));
        }


        // 🔹 Escolhe aleatoriamente uma direção válida
        return validas.get(new Random().nextInt(validas.size()));
    }


    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }
    public Color getCor() { return cor; }
    public boolean isAtivo() { return ativo; }
    
    private boolean reservarCaminho(List<Point> path, long timeoutPorCelulaMs) throws InterruptedException {
        // Ordem consistente para reduzir deadlocks: ordenar por (row, col)
        List<Point> ordered = new ArrayList<>(path);
        ordered.sort(Comparator.comparingInt((Point p) -> p.x).thenComparingInt(p -> p.y));

        List<Point> adquiridas = new ArrayList<>();
        try {
            for (Point p : ordered) {
                Celula cel = malha.getCelula(p.x, p.y);
                boolean ok = cel.tentarEntrar(timeoutPorCelulaMs);
                if (!ok) {
                    return false;
                }
                adquiridas.add(p);
            }
            return true;
        } finally {
            if (!adquiridas.isEmpty() && adquiridas.size() != ordered.size()) {
                // se não adquiriu todas, libera as já obtidas
                for (Point p : adquiridas) {
                    malha.getCelula(p.x, p.y).sair();
                }
            }
    }
}
}