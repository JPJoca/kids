package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileScanTask implements Callable<String> {
    private final Path filePath;
    private final char letter;
    private final double min;
    private final double max;
    private final String jobName;
    boolean wasInterrupted = false;
    private final Lock readLock ;

    public FileScanTask(Path file, char letter, double min, double max, String jobName, ReentrantReadWriteLock rwLock) {
        this.readLock  = rwLock.readLock();
        this.filePath = file;
        this.letter = letter;
        this.min = min;
        this.max = max;
        this.jobName = jobName;
    }

    @Override
    public String call() {
        String tmpFileName = "tmp_" + jobName + "_" + filePath.getFileName().toString() + ".tmp";
        boolean isCSV = filePath.toString().endsWith(".csv");
        boolean wasInterrupted = false;

        readLock.lock();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(new FileWriter(tmpFileName, false))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[SCAN] Prekinut task, brišem fajl: " + tmpFileName);
                    break;
                }

                if (isCSV && firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 2) continue;

                String station = parts[0].trim();
                double temp = Double.parseDouble(parts[1].trim());

                if (Character.toLowerCase(station.charAt(0)) == letter &&
                        temp >= min && temp <= max) {
                    writer.println(station + ";" + temp);
                }
            }

        } catch (Exception e) {
            System.out.println("[UPOZORENJE] Preskačem fajl: " + filePath.getFileName());
        }  finally {
        readLock.unlock();
        }



        return tmpFileName;
    }

}