package com.example;

import com.example.api.ElpriserAPI;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();
    }

    private static Prisklass getZone() {
        Prisklass zone = null;
        while (zone == null) {
            String zoneInput = System.console().readLine("Ange elområde (SE1, SE2, SE3, SE4): ").trim().toUpperCase();
            try {
                zone = Prisklass.valueOf(zoneInput);
            } catch (IllegalArgumentException e) {
                System.err.print("Ogiltigt värde, försök igen: ");
            }
        }
        return zone;
    }
}