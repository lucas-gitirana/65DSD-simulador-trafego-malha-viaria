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
    public void entrar() throws InterruptedException {
        while (ocupada) {
            wait();
        }
        ocupada = true;
    }

    @Override
    public void sair() {
        ocupada = false;
        notifyAll();
    }

    @Override
    public boolean isOcupada() {
        return ocupada;
    }
    
}
