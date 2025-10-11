/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

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

    private List<Point> pathReservado = null;

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
        if (pathReservado != null && !pathReservado.isEmpty()) {
            // Continuar movendo pelo caminho reservado
            Point destino = pathReservado.get(0);
            int proximaLinha = destino.y;
            int proximaColuna = destino.x;

            Celula proximaCelula = malha.getCelula(proximaLinha, proximaColuna);

            if (proximaCelula == null) {
                ativo = false;
                return;
            }

            // Como já reservado, não precisa tentarEntrar novamente
            int oldLinha = this.linha;
            int oldColuna = this.coluna;

            this.linha = proximaLinha;
            this.coluna = proximaColuna;
            this.celulaAtual.sair();
            ultimaPosicao = new Point(oldColuna, oldLinha);
            this.celulaAtual = proximaCelula;

            pathReservado.remove(0);
            if (pathReservado.isEmpty()) {
                pathReservado = null;
            }
            return;
        }

        int proximaLinha = linha;
        int proximaColuna = coluna;

        TipoCelula tipoAtual = this.celulaAtual.getTipo();

        boolean isCurrentCruzamento = malha.isCruzamento(tipoAtual);

        if (!isCurrentCruzamento) {
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

        boolean needsReservation = malha.isCruzamento(proximaCelula.getTipo()) || isCurrentCruzamento;

        if (needsReservation) {
            // Lógica para cruzamentos: decidir e reservar caminho completo
            Point startPoint = isCurrentCruzamento ? new Point(coluna, linha) : new Point(proximaColuna, proximaLinha);
            Point anterior = isCurrentCruzamento ? ultimaPosicao : new Point(coluna, linha);

            Set<Point> visited = new HashSet<>();
            List<List<Point>> possiblePaths = encontrarCaminhosPossiveis(startPoint, anterior, visited);

            if (possiblePaths.isEmpty()) {
                System.out.println("Veículo morreu no cruzamento (" + linha + "," + coluna + ") - sem saída livre.");
                ativo = false;
                return;
            }

            // Embaralhar para escolha aleatória
            Collections.shuffle(possiblePaths, rnd);

            boolean reservado = false;
            for (List<Point> path : possiblePaths) {
                if (reservarCaminho(path, TIMEOUT_POR_CELULA_MS)) {
                    pathReservado = new ArrayList<>(path);
                    reservado = true;
                    break;
                }
            }

            if (!reservado) {
                // Backoff e tenta na próxima iteração
                Thread.sleep(BACKOFF_MIN_MS + rnd.nextInt(BACKOFF_MAX_MS - BACKOFF_MIN_MS));
                return;
            }

            // Agora mover para o primeiro do path (se current já é start, então path começa com próximo)
            if (isCurrentCruzamento) {
                // Já está no start, então o path inclui o current como primeiro? Não, o path é from start, mas como já está, move para o próximo
                // Mas encontrarCaminhosPossiveis inclui start como primeiro no path
                // Então, como já está no start, remove(0) e move para o próximo se não vazio
                Point first = pathReservado.get(0);
                if (first.y != linha || first.x != coluna) {
                    // Erro
                    ativo = false;
                    return;
                }
                pathReservado.remove(0);
                if (pathReservado.isEmpty()) {
                    pathReservado = null;
                    // Nada a mover, mas deve mover para fora? Se path tinha só [start], mas se só start, significa no cruzamento sem saida, mas filtered
                    return;
                }
            }

            // Mover para o próximo (primeiro do path agora)
            Point destino = pathReservado.get(0);
            proximaLinha = destino.y;
            proximaColuna = destino.x;
            proximaCelula = malha.getCelula(proximaLinha, proximaColuna);

            int oldLinha = this.linha;
            int oldColuna = this.coluna;

            this.linha = proximaLinha;
            this.coluna = proximaColuna;
            this.celulaAtual.sair();
            ultimaPosicao = new Point(oldColuna, oldLinha);
            this.celulaAtual = proximaCelula;

            pathReservado.remove(0);
            if (pathReservado.isEmpty()) {
                pathReservado = null;
            }
            return;
        }

        // Movimento normal fora de cruzamentos
        boolean entrou = proximaCelula.tentarEntrar(TIMEOUT_POR_CELULA_MS);
        if (!entrou) {
            // Backoff e tenta na próxima iteração
            Thread.sleep(BACKOFF_MIN_MS + rnd.nextInt(BACKOFF_MAX_MS - BACKOFF_MIN_MS));
            return;
        }

        int oldLinha = this.linha;
        int oldColuna = this.coluna;

        this.linha = proximaLinha;
        this.coluna = proximaColuna;
        this.celulaAtual.sair();
        ultimaPosicao = new Point(oldColuna, oldLinha);
        this.celulaAtual = proximaCelula;
    }

    private List<List<Point>> encontrarCaminhosPossiveis(Point entrada, Point anterior, Set<Point> visited) {
        List<List<Point>> paths = new ArrayList<>();

        if (visited.contains(entrada)) {
            return paths;
        }

        visited.add(entrada);

        Celula cel = malha.getCelula(entrada.y, entrada.x);
        TipoCelula tipo = cel.getTipo();

        if (!malha.isCruzamento(tipo)) {
            visited.remove(entrada);
            return paths;
        }

        List<Point> opcoes = getDirecoesPossiveis(entrada.y, entrada.x, anterior);

        for (Point prox : opcoes) {
            Celula proxCel = malha.getCelula(prox.y, prox.x);

            if (malha.isCruzamento(proxCel.getTipo())) {
                Set<Point> newVisited = new HashSet<>(visited);
                List<List<Point>> subPaths = encontrarCaminhosPossiveis(prox, entrada, newVisited);
                for (List<Point> sub : subPaths) {
                    List<Point> full = new ArrayList<>();
                    full.add(entrada);
                    full.addAll(sub);
                    paths.add(full);
                }
            } else {
                // Saída para estrada
                List<Point> path = new ArrayList<>();
                path.add(entrada);
                path.add(prox);
                paths.add(path);
            }
        }

        visited.remove(entrada);
        return paths;
    }

    private List<Point> getDirecoesPossiveis(int lin, int col, Point anterior) {
        Celula cel = malha.getCelula(lin, col);
        TipoCelula tipo = cel.getTipo();

        List<Point> opcoes = new ArrayList<>();

        // Copiado da lógica original de decidirProximoPassoCruzamento
        switch (tipo) {
            case CRUZAMENTO_CIMA:
            case CRUZAMENTO_CIMA_DIREITA:
            case CRUZAMENTO_CIMA_ESQUERDA:
                opcoes.add(new Point(col, lin - 1)); // cima
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_BAIXO:
            case CRUZAMENTO_BAIXO_ESQUERDA:
            case CRUZAMENTO_DIREITA_BAIXO:
                opcoes.add(new Point(col, lin + 1)); // baixo
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_DIREITA:
            case CRUZAMENTO_CIMA_DIREITA:
            case CRUZAMENTO_DIREITA_BAIXO:
                opcoes.add(new Point(col + 1, lin)); // direita
                break;
        }
        switch (tipo) {
            case CRUZAMENTO_ESQUERDA:
            case CRUZAMENTO_CIMA_ESQUERDA:
            case CRUZAMENTO_BAIXO_ESQUERDA:
                opcoes.add(new Point(col - 1, lin)); // esquerda
                break;
        }

        // Remover anterior se existir
        if (anterior != null) {
            opcoes.removeIf(p -> p.equals(anterior));
        }

        // Filtrar válidas: dentro dos limites e não vazias (mas não checar ocupada, pois vamos reservar)
        opcoes.removeIf(p -> {
            if (p.y < 0 || p.y >= malha.getLinhas() || p.x < 0 || p.x >= malha.getColunas()) {
                return true;
            }
            Celula c = malha.getCelula(p.y, p.x);
            return c == null || c.getTipo() == TipoCelula.VAZIO;
        });

        return opcoes;
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
                Celula cel = malha.getCelula(p.y, p.x);
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
                    malha.getCelula(p.y, p.x).sair();
                }
            }
        }
    }
}