/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.dsd.simulador.trafego;

import com.mycompany.dsd.simulador.trafego.model.Malha;
import java.io.IOException;

public class App {

    public static void main(String[] args) {
        try {
            Malha malha = new Malha("/malhas/malha-exemplo-1.txt");
            malha.imprimirMalha();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
