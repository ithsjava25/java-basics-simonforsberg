package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        String zone = null;
        LocalDate date = LocalDate.now();
        boolean sorted = false;
        String charging = null;

        // --- Om argument saknas, skriv ut hjälp ---
        if (args.length == 0) {
            printHelp();
            return;
        }

        //  --- Hantera kommandoradsargument ---
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> {
                    if (i + 1 < args.length) {
                        String inputZone = args[++i].toUpperCase();
                        switch (inputZone) {
                            case "SE1", "SE2", "SE3", "SE4" -> zone = inputZone;
                            default -> {
                                System.out.println("Ogiltig zon: " + inputZone);
                                return;
                            }
                        }
                    } else {
                        System.out.println("Ogiltig zon: värde saknas efter --zone");
                        return;
                    }
                }
                case "--date" -> {
                    if (i + 1 < args.length) {
                        String inputDate = args[++i];
                        try {
                            date = LocalDate.parse(inputDate);
                        } catch (java.time.format.DateTimeParseException e) {
                            System.out.println("Ogiltigt datum: " + inputDate);
                            printHelp();
                            return;
                        }
                    } else {
                        System.out.println("Ogiltigt datum: värde saknas efter --date");
                        printHelp();
                        return;
                    }
                }
                case "--sorted" -> sorted = true;
                case "--charging" -> {
                    if (i + 1 < args.length) {
                        charging = args[++i];
                        switch (charging) {
                            case "2h", "4h", "8h" -> {
                            }
                            default -> {
                                System.out.println("Ogiltigt laddningsalternativ: " + charging);
                                printHelp();
                                return;
                            }
                        }
                    } else {
                        System.out.println("Ogiltigt laddningsalternativ: värde saknas efter --charging");
                        printHelp();
                        return;
                    }
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
                default -> {
                    System.out.println("Okänt argument: " + args[i]);
                    printHelp();
                    return;
                }
            }
        }
        if (zone == null) {
            System.out.println("Fel: --zone måste anges!");
            printHelp();
            return;
        }

        //  --- Hämta priser ---
        ElpriserAPI api = new ElpriserAPI();

        List<Elpris> todayList = api.getPriser(date, Prisklass.valueOf(zone));
        Elpris[] todayPrices = todayList.toArray(new Elpris[0]);

        List<Elpris> tomorrowList = api.getPriser(date.plusDays(1), Prisklass.valueOf(zone));
        Elpris[] tomorrowPrices = tomorrowList.toArray(new Elpris[0]);

        //  --- Testa om priser finns ---
        if (todayPrices.length == 0) {
            System.out.println("Inga priser tillgängliga för " + zone + " på " + date);
            return;
        }

        // --- Sortera priser i fallande ordning ---
        // if (sorted) sortDescending(todayPrices);

        // --- Sortera priser i stigande ordning ---
        if (sorted) sortAscending(todayPrices);

        // --- Skriv ut dagens priser ---
        printPrices(zone, date, todayPrices);

        // --- Skriv ut medel/lägsta/högsta priser ---
        printStats(todayPrices, date);

        // --- Optimalt laddningsfönster ---
        if (charging != null) {
            printChargingWindow(charging, todayPrices, tomorrowPrices);
        }

    }

    private static void printChargingWindow(String charging, Elpris[] todayPrices, Elpris[] tomorrowPrices) {
        int hours;
        switch (charging) {
            case "2h" -> hours = 2;
            case "4h" -> hours = 4;
            case "8h" -> hours = 8;
            default -> {
                System.out.println("Ogiltigt laddningsalternativ: endast 2h, 4h eller 8h tillåtet.");
                return;
            }
        }

        // Kombinera dagens + morgondagens priser
        Elpris[] combinedPrices = combineArrays(todayPrices, tomorrowPrices);

        // Beräkna optimalt laddningsfönster
        Elpris[] window = calculateOptimalWindow(combinedPrices, hours);

        // Beräkna medelpris för laddningsfönster
        double windowMean = calculateMean(window);

        // Hämta starttid för första timmen i fönstret
        String startTimeStr = String.format("%02d:%02d", window[0].timeStart().getHour(), window[0].timeStart().getMinute());

        // Skriv ut laddningsfönster med starttid
        System.out.printf("%nPåbörja laddning kl %s för %dh:%n", startTimeStr, hours);
        for (Elpris p : window) {
            String timeStr = String.format("%02d:%02d", p.timeStart().getHour(), p.timeStart().getMinute());
            String oreStr = formatOre(p.sekPerKWh());
            System.out.printf("%s %s öre%n", timeStr, oreStr);
        }
        System.out.printf("Medelpris för fönster: %s öre%n", formatOre(windowMean));
    }

    private static void printStats(Elpris[] todayPrices, LocalDate date) {
        // Beräkna medelpris för dagen
        double mean = calculateMean(todayPrices);
        // Hitta lägsta och högsta timpris för dagen
        Elpris minHour = findMin(todayPrices);
        Elpris maxHour = findMax(todayPrices);

        // Skriv ut resultat
        System.out.printf("Medelpris för %s: %s öre%n", date, formatOre(mean));
        System.out.printf("Lägsta pris: %02d-%02d - %s öre%n",
                minHour.timeStart().getHour(),
                minHour.timeEnd().getHour(),
                formatOre(minHour.sekPerKWh()));
        System.out.printf("Högsta pris: %02d-%02d - %s öre%n",
                maxHour.timeStart().getHour(),
                maxHour.timeEnd().getHour(),
                formatOre(maxHour.sekPerKWh()));
    }

    private static void printPrices(String zone, LocalDate date, Elpris[] todayPrices) {
        System.out.println("Elpriser för " + zone + " (" + date + "):");
        for (Elpris p : todayPrices) {
            int startHour = p.timeStart().getHour();
            int endHour = p.timeEnd().getHour();
            String priceStr = formatOre(p.sekPerKWh());
            System.out.printf("%02d-%02d %s öre%n", startHour, endHour, priceStr);
        }
    }

    private static double calculateMean(Elpris[] priser) {
        double sum = 0;
        for (Elpris p : priser) sum += p.sekPerKWh();
        return sum / priser.length;
    }

    private static Elpris findMin(Elpris[] priser) {
        Elpris min = priser[0];
        for (Elpris p : priser) if (p.sekPerKWh() < min.sekPerKWh()) min = p;
        return min;
    }

    private static Elpris findMax(Elpris[] priser) {
        Elpris max = priser[0];
        for (Elpris p : priser) if (p.sekPerKWh() > max.sekPerKWh()) max = p;
        return max;
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

    private static void sortAscending(Elpris[] priser) {
        for (int i = 0; i < priser.length - 1; i++) {
            for (int j = 0; j < priser.length - 1 - i; j++) {
                if (priser[j].sekPerKWh() > priser[j + 1].sekPerKWh()) {
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

    private static Elpris[] calculateOptimalWindow(Elpris[] priser, int hours) {
        if (priser.length < hours) return priser;

        double minSum = Double.MAX_VALUE;
        int startIndex = 0;

        for (int i = 0; i <= priser.length - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) sum += priser[i + j].sekPerKWh();
            if (sum < minSum) {
                minSum = sum;
                startIndex = i;
            }
        }
        Elpris[] window = new Elpris[hours];
        System.arraycopy(priser, startIndex, window, 0, hours);
        return window;
    }

    private static String formatOre(double sekPerKWh) {
        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("sv-SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp target/classes com.example.Main [options]");
        System.out.println("Options:");
        System.out.println("  --zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("  --date YYYY-MM-DD        (optional, defaults to current date)");
        System.out.println("  --sorted                 (optional, to display prices in descending order)");
        System.out.println("  --charging 2h|4h|8h      (optional, to find optimal charging window)");
        System.out.println("  --help                   (optional, to display usage information)");
    }
}