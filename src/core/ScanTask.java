package core;

import cli.CommandProcessor;
import model.JobStatus;
import util.FileScanTask;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class ScanTask implements Runnable {
    private final String jobName;
    private final char letter;
    private final double min;
    private final double max;
    private final String outputFile;
    private final String dataDir;
    private final JobManager jobManager;
    private final ExecutorService executorService;
    private final CommandProcessor commandProcessor;
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock writeLock = rwLock.writeLock();



    public ScanTask(String jobName, char letter, double min, double max,
                    String outputFile, String dataDir, JobManager jobManager, ExecutorService executorService, CommandProcessor commandProcessor) {
        this.jobName = jobName;
        this.letter = Character.toLowerCase(letter);
        this.min = min;
        this.max = max;
        this.outputFile = outputFile;
        this.dataDir = dataDir;
        this.jobManager = jobManager;
        this.executorService = executorService;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        jobManager.updateStatus(jobName, JobStatus.RUNNING.name());

        try {
            List<Future<String>> futures = new ArrayList<>();

            try (Stream<Path> files = Files.list(Paths.get(dataDir))) {
                files.filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".csv"))
                        .forEach(file -> {
                            Future<String> future = executorService.submit(
                                    new FileScanTask(file, letter, min, max, jobName,rwLock));
                            futures.add(future);
                        });
            }

            writeLock.lock();

            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, false))) {
                for (Future<String> future : futures) {
                    String tmpPath = future.get();
                    try (BufferedReader reader = new BufferedReader(new FileReader(tmpPath))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.println(line);
                        }
                    } catch (IOException e) {
                        System.out.println("[GREŠKA] Ne mogu da pročitam privremeni fajl: " + tmpPath);
                    }
                    Files.deleteIfExists(Path.of(tmpPath));
                }
            }

        } catch (Exception e) {
            System.out.println("[GREŠKA] Došlo je do greške tokom SCAN posla: " + jobName);
        } finally {
            writeLock.unlock();
        }

        jobManager.updateStatus(jobName, JobStatus.COMPLETED.name());
        commandProcessor.removeActiveScanCommand(jobName);
        System.out.println("[SCAN] Job '" + jobName + "' završen.");
    }
}