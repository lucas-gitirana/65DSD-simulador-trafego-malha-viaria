package com.mycompany.dsd.simulador.trafego.controller;

import com.mycompany.dsd.simulador.trafego.model.Celula;
import com.mycompany.dsd.simulador.trafego.model.Malha;
import com.mycompany.dsd.simulador.trafego.model.Veiculo;
import java.awt.Point;
import java.util.List;
import java.util.Random;
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
    private final Random random = new Random();

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

            veiculos.removeIf(v -> !v.isAtivo());

            if (inserindo && veiculos.size() < maxVeiculos) {
                List<Point> pontosDeEntrada = malha.getPontosDeEntrada();

                if (!pontosDeEntrada.isEmpty()) {
                    Point pontoInicial = pontosDeEntrada.get(random.nextInt(pontosDeEntrada.size()));

                    Celula celulaInicial = malha.getCelula(pontoInicial.y, pontoInicial.x);

                    if (!celulaInicial.isOcupada()) {
                        Veiculo v = new Veiculo(malha, pontoInicial.y, pontoInicial.x, 500 + random.nextInt(500));
                        veiculos.add(v);
                        v.start();
                    }
                }
            }

            try {
                Thread.sleep(intervaloInsercao);
            } catch (InterruptedException e) {
                rodando = false;
            }
        }
        for (Veiculo v : veiculos) {
            v.interrupt();
        }
    }
}