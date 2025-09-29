package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleBiFunction;

public class Main {
    public static void main(String[] args) {
        String zone = null; // variabel för prisområden, sätt som null
        LocalDate date = LocalDate.now(); // variabel för datum, satt som dagens datum om inget annat anges
        boolean sorted = false; // variabel för sorteringsmetod, gör ej om ej anropad/kör om true
        String charging = null; // variabel för laddningsmetod, gör ej om ej anropad

        // TODO Konsekventa felmeddelanden

        // TODO Se över flödet

        //  --- Check command line argument(s) ---
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
                        System.out.println("Ogiltig zon");
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
                        System.out.println("Ogiltigt datum: saknas värde efter --date");
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
                        System.out.println("Ogiltigt laddningsalternativ: saknas värde efter --charging");
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
        Elpris[] todaysPrices = todayList.toArray(new Elpris[0]);

        List<Elpris> tomorrowList = api.getPriser(date.plusDays(1), Prisklass.valueOf(zone));
        Elpris[] tomorrowsPrices = tomorrowList.toArray(new Elpris[0]);

        //  --- Testa om priser hittas ---
        if (todaysPrices.length == 0) {
            System.out.println("Inga priser tillgängliga för " + zone + " på " + date);
            return;
        }

        // --- Sortera priserna i fallande/STIGANDE? ordning ---
        if (sorted) sortAscending(todaysPrices);

        // if (sorted) sortDescending(todaysPrices);

        // --- Skriv ut dagens priser ---
        System.out.println("Elpriser för " + zone + " (" + date + "):");
        for (Elpris p : todaysPrices) {
            int startHour = p.timeStart().getHour();
            int endHour = p.timeEnd().getHour();
            String priceStr = formatOre(p.sekPerKWh());
            System.out.printf("%02d-%02d %s öre%n", startHour, endHour, priceStr);
        }

        // --- Medelpris för dagen ---
        double mean = calculateMean(todaysPrices);
        System.out.printf("Medelpris för %s: %s öre%n", date, formatOre(mean));

        // --- Minsta och högsta timpris för dagen ---
        Elpris minHour = findMin(todaysPrices);
        Elpris maxHour = findMax(todaysPrices);
        System.out.printf("Lägsta pris: %02d-%02d - %s öre%n",
                minHour.timeStart().getHour(),
                minHour.timeEnd().getHour(),
                formatOre(minHour.sekPerKWh()));
        System.out.printf("Högsta pris: %02d-%02d - %s öre%n",
                maxHour.timeStart().getHour(),
                maxHour.timeEnd().getHour(),
                formatOre(maxHour.sekPerKWh()));

        // --- Optimal laddning om --charging används ---
        if (charging != null) {
            int hours;
            switch (charging) {
                case "2h" -> hours = 2;
                case "4h" -> hours = 4;
                case "8h" -> hours = 8;
                default -> {
                    System.out.println("Fel: Okänt laddningsalternativ. Endast 2h, 4h eller 8h tillåtet.");
                    return;
                }
            }
            // // Kombinera dagens + morgondagens priser för laddning
            Elpris[] combinedPrices = combineArrays(todaysPrices, tomorrowsPrices);

            Elpris[] window = findOptimalWindow(combinedPrices, hours);

            // Medelpris för laddningsfönstret
            double windowMean = calculateMean(window);

            // Hämta starttid för första timmen i fönstret
            String startTimeStr = String.format("%02d:%02d", window[0].timeStart().getHour(), window[0].timeStart().getMinute());

            // Skriv ut med starttid
            System.out.printf("%nPåbörja laddning kl %s för %dh:%n", startTimeStr, hours);
            for (Elpris p : window) {
                String timeStr = String.format("%02d:%02d", p.timeStart().getHour(), p.timeStart().getMinute());
                String oreStr = formatOre(p.sekPerKWh());
                System.out.printf("%s %s öre%n", timeStr, oreStr);
            }
            System.out.printf("Medelpris för fönster: %s öre%n", formatOre(windowMean));
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

    private static String formatOre(double sekPerKWh) {
        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("sv-SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }

    private static Elpris[] combineArrays(Elpris[] array1, Elpris[] array2) {
        Elpris[] combined = new Elpris[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    // --- Sliding Window för optimal laddning ---
    private static Elpris[] findOptimalWindow(Elpris[] priser, int hours) {
        if (priser.length < hours) return priser; // returnera allt om för få timmar

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