package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileProcessor implements Runnable {

    private final Path filePath;

    public FileProcessor(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run() {
        boolean isCSV = filePath.toString().endsWith(".csv");

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[FILEPROCESSOR] Prekinut tokom čitanja fajla: " + filePath.getFileName());
                    return;
                }

                if (isCSV && firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 2) continue;

                String station = parts[0].trim();
                double temperature = Double.parseDouble(parts[1].trim());
                if (station.isEmpty()) continue;

                MapAggregator.getInstance().update(station, temperature);
            }

            System.out.println("[INFO] Obrada završena: " + filePath.getFileName());

        } catch (IOException e) {
            System.out.println("[GREŠKA] Ne mogu da pročitam fajl: " + filePath.getFileName());
        }
    }
}
