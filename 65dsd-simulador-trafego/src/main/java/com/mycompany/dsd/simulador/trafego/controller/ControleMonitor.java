/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.controller;

/**
 *
 * @author gitir
 */
public class ControleMonitor implements ControleCelula {
    private boolean ocupada = false;

    @Override
    public synchronized void entrar() throws InterruptedException {
        while (ocupada) {
            wait();
        }
        ocupada = true;
    }

    @Override
    public synchronized void sair() {
        ocupada = false;
        notifyAll();
    }

    @Override
    public boolean isOcupada() {
        return ocupada;
    }

    @Override
    public boolean tentarEntrar(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (this) {
            while (ocupada) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) return false;
                wait(remaining);
            }
            ocupada = true;
            return true;
        }
    }
    
}
