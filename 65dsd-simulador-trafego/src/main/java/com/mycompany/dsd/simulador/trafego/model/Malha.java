package com.mycompany.dsd.simulador.trafego.model;

import com.mycompany.dsd.simulador.trafego.controller.ControleCelula;
import com.mycompany.dsd.simulador.trafego.controller.ControleMonitor;
import com.mycompany.dsd.simulador.trafego.controller.ControleSemaforo;
import java.awt.Point; // Importação necessária
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList; // Importação necessária
import java.util.List; // Importação necessária

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
}