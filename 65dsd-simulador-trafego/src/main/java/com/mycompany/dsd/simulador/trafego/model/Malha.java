package com.mycompany.dsd.simulador.trafego.model;

import com.mycompany.dsd.simulador.trafego.controller.ControleCelula;
import com.mycompany.dsd.simulador.trafego.controller.ControleMonitor;
import com.mycompany.dsd.simulador.trafego.controller.ControleSemaforo;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Malha {
    private int linhas;
    private int colunas;
    private Celula[][] grid;

    private List<Point> pontosDeEntrada;

    public Malha(String path, boolean usarSemaforos) throws IOException {
        carregarMalha(path, usarSemaforos);
    }

    public int getLinhas() { return linhas; }
    public int getColunas() { return colunas; }

    public List<Point> getPontosDeEntrada() {
        return pontosDeEntrada;
    }

    private void carregarMalha(String path, boolean usarSemaforos) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException("Arquivo não encontrado no classpath: " + path);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                linhas = Integer.parseInt(br.readLine().trim());
                colunas = Integer.parseInt(br.readLine().trim());
                grid = new Celula[linhas][colunas];

                for (int i = 0; i < linhas; i++) {
                    String[] valores = br.readLine().trim().split("\\s+");
                    for (int j = 0; j < colunas; j++) {
                        int valor = Integer.parseInt(valores[j]);
                        TipoCelula tipo = TipoCelula.fromInt(valor);
                        ControleCelula controle = usarSemaforos ?
                                new ControleSemaforo() : new ControleMonitor();

                        grid[i][j] = new Celula(tipo, controle);
                    }
                }
            }
        }

        identificarPontosDeEntrada();
    }

    private void identificarPontosDeEntrada() {
        this.pontosDeEntrada = new ArrayList<>();

        for (int j = 0; j < colunas; j++) {
            if (grid[0][j].getTipo() == TipoCelula.ESTRADA_BAIXO) {
                pontosDeEntrada.add(new Point(j, 0));
            }
            if (grid[linhas - 1][j].getTipo() == TipoCelula.ESTRADA_CIMA) {
                pontosDeEntrada.add(new Point(j, linhas - 1));
            }
        }

        for (int i = 1; i < linhas - 1; i++) {
            if (grid[i][0].getTipo() == TipoCelula.ESTRADA_DIREITA) {
                pontosDeEntrada.add(new Point(0, i));
            }
            if (grid[i][colunas - 1].getTipo() == TipoCelula.ESTRADA_ESQUERDA) {
                pontosDeEntrada.add(new Point(colunas - 1, i));
            }
        }
    }

    public Celula getCelula(int linha, int coluna) {
        if (linha < 0 || linha >= linhas || coluna < 0 || coluna >= colunas)
            return null;

        return grid[linha][coluna];
    }

    public void imprimirMalha() {
        for (int i = 0; i < linhas; i++) {
            for (int j = 0; j < colunas; j++) {
                System.out.println(grid[i][j].getTipo().getValor() + " ");
            }
            System.out.println("");
        }
    }
    
    public boolean isCruzamento(TipoCelula t) {
        return t.ordinal() >= TipoCelula.CRUZAMENTO_CIMA.ordinal() &&
               t.ordinal() <= TipoCelula.CRUZAMENTO_BAIXO_ESQUERDA.ordinal();
    }
    
    /**
    * Retorna os deltas (dr, dc) possíveis de saída a partir da célula de cruzamento,
    * excluindo a direção de onde o veículo veio (entryFromRow, entryFromCol).
    * Cada delta é um int[2] {dr, dc}.
    */
    public List<int[]> getPossibleExitDeltas(int crossRow, int crossCol, int entryFromRow, int entryFromCol) {
        List<int[]> exits = new ArrayList<>();
        // 4 direções: cima(-1,0), direita(0,+1), baixo(+1,0), esquerda(0,-1)
        int[][] all = { {-1,0}, {0,1}, {1,0}, {0,-1} };
        for (int[] d : all) {
            int nr = crossRow + d[0];
            int nc = crossCol + d[1];
            if (nr < 0 || nr >= linhas || nc < 0 || nc >= colunas) continue;
            // não voltar para a célula de onde veio
            if (nr == entryFromRow && nc == entryFromCol) continue;
            Celula c = getCelula(nr, nc);
            if (c != null && c.getTipo() != TipoCelula.VAZIO) {
                // consideramos como saída válida
                exits.add(d);
            }
        }
        return exits;
    }
    
    /**
    * Retorna o caminho (lista de Points) desde a primeira célula de cruzamento
    * até a primeira célula que não seja de cruzamento (inclui essa célula final).
    * startCrossRow/startCrossCol = posição da primeira célula de cruzamento (onde entra)
    * outDelta = direção escolhida para sair do cruzamento
    */
    public List<Point> getPathThroughCrossing(int startCrossRow, int startCrossCol, int outDr, int outDc) {
        List<Point> path = new ArrayList<>();
        int r = startCrossRow;
        int c = startCrossCol;

        // percorre células enquanto forem de cruzamento (inclui primeira)
        while (r >= 0 && r < linhas && c >= 0 && c < colunas && isCruzamento(getCelula(r, c).getTipo())) {
            path.add(new Point(r, c));
            r += outDr;
            c += outDc;
        }

        // agora r,c é a primeira célula fora do conjunto de cruzamento: incluir se válida e não vazia
        if (r >= 0 && r < linhas && c >= 0 && c < colunas) {
            Celula finalCel = getCelula(r, c);
            if (finalCel != null && finalCel.getTipo() != TipoCelula.VAZIO) {
                path.add(new Point(r, c));
            }
        }
        return path;
    }
}