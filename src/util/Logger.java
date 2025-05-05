package util;

import model.StationData;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

public class Logger {

    public static synchronized void exportSnapshot(Map<Character, StationData> snapshot){
        try (PrintWriter out = new PrintWriter(new FileWriter("map_export.csv", false))) {
            out.println("Letter,Station count,Sum");

            snapshot.keySet().stream().sorted().forEach(letter -> {
                var data = snapshot.get(letter);
                out.println(letter + "," + data.count() + "," + data.sum());
            });


        } catch (Exception e) {
            System.out.println("Greška pri eksportovanju mape.");
        }
    }
}
