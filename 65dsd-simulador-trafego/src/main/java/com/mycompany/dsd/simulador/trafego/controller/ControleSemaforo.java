/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.controller;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author gitir
 */
public class ControleSemaforo implements ControleCelula {
    private final Semaphore semaforo = new Semaphore(1);

    @Override
    public void entrar() throws InterruptedException {
        semaforo.acquire();
    }

    @Override
    public void sair() {
        semaforo.release();
    }

    @Override
    public boolean isOcupada() {
        return semaforo.availablePermits() == 0;
    }

    @Override
    public boolean tentarEntrar(long timeoutMs) throws InterruptedException {
        return semaforo.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
}
