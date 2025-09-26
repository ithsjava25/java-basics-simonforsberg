package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        String zone = null; // variabel för prisområden, sätt som null
        LocalDate date = LocalDate.now(); // variabel för datum, satt som dagens datum om inget annat anges
        boolean sorted = false; // varibel för sorteringsmetod, gör ej om ej anropad
        String charging = null; // variabel för laddningsmetod, gör ej om ej anropad

        // Check command line argument(s)
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    zone = args[++i].toUpperCase();
                    break;
                    /*
                    // Lägg till??
                    if (zone == null) {
                    System.err.println("Fel: --zone måste anges!");
                    printHelp();
                    return;
                    }
                    */
                case "--date":
                    date = LocalDate.parse(args[++i]);
                    break;
                case "--sorted":
                    sorted = true;
                    break;
                case "--charging":
                    charging = args[++i];
                    break;
                case "--help":
                    printHelp();
                    return;
                default:
                    break;
            }
        }

        ElpriserAPI api = new ElpriserAPI();

    }

    private static void sortDescending(Elpris[] priser) {
        for (int i = 0; i < priser.length - 1; i++) {
            for (int j = 0; j < priser.length - 1 - i; j++) {
                if (priser[j].sekPerKWh() < priser[j + 1].sekPerKWh()) {
                    Elpris temp = priser[j];
                    priser[j] = priser[j + 1];
                    priser[j + 1] = temp;
                }
            }
        }
    }

    private static Elpris[] combineArrays(Elpris[] array1, Elpris[] array2) {
        Elpris[] combined = new Elpris[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp target/classes com.example.Main [options]");
        System.out.println("Options:");
        System.out.println("--zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("--date YYYY-MM-DD        (optional, defaults to current date)");
        System.out.println("--sorted                 (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h      (optional, to find optimal charging windows)");
        System.out.println("--help                   (optional, to display usage information)");
    }
}