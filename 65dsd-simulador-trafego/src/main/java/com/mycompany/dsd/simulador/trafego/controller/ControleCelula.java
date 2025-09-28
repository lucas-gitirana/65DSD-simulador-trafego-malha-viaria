/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.dsd.simulador.trafego.controller;

/**
 *
 * @author gitir
 */
public interface ControleCelula {
    void entrar() throws InterruptedException;
    void sair();
    boolean isOcupada();
}
