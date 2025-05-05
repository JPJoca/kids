package cli;

import core.JobManager;
import core.MapAggregator;
import core.ScanTask;
import model.Command;
import model.StationData;
import util.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class CommandProcessor extends Thread {
    private final BlockingQueue<Command> commandQueue;
    private final JobManager jobManager;
    private final MapAggregator mapAggregator;
    private final ExecutorService executorService;
    private final String dataDirectory;

    private final Map<String, Command> activeScanCommands = new ConcurrentHashMap<>();


    public CommandProcessor(BlockingQueue<Command> commandQueue,
                            JobManager jobManager,
                            MapAggregator mapAggregator,
                            ExecutorService executorService,
                            String dataDirectory) {
        this.commandQueue = commandQueue;
        this.jobManager = jobManager;
        this.mapAggregator = mapAggregator;
        this.executorService = executorService;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Command command = commandQueue.take();
                process(command);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[CPROCESSOR] ComandProcessor je ugašen.");
            }
        }
    }

    private void process(Command command) {
        switch (command.getName()) {
            case "SCAN" -> handleScan(command);
            case "STATUS" -> handleStatus(command);
            case "MAP" -> handleMap();
            case "EXPORTMAP" -> handleExportMap();
            default -> System.out.println("Nepoznata komanda: " + command.getName());
        }
    }

    private void handleScan(Command command) {
        List<String> requiredArgs = List.of("min", "max", "letter", "output", "job");
        List<String> missing = new ArrayList<>();

        for (String arg : requiredArgs) {
            if (!command.hasArg(arg)) {
                missing.add(arg);
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("[SCAN] komandai nedostaju argumenti: " + String.join(", ", missing));
            return;
        }

        try {
            double min = Double.parseDouble(command.getArg("min"));
            double max = Double.parseDouble(command.getArg("max"));
            String letter = command.getArg("letter");
            String outputFile = command.getArg("output");
            String jobName = command.getArg("job");

            if (letter.length() != 1) {
                System.out.println("[SCAN]Greška: --letter mora biti jedno slovo.");
                return;
            }

            if(jobManager.isJobRegistered(jobName))
                return;

            jobManager.registerJob(jobName);

            activeScanCommands.put(jobName, command);

            Runnable scanTask = new ScanTask(
                    jobName, letter.charAt(0), min, max, outputFile, dataDirectory, jobManager, executorService, this);
            executorService.submit(scanTask);
            System.out.println("[SCAN] Job '" + jobName + "' je pokrenut.");

        } catch (NumberFormatException e) {
            System.out.println("Greška: --min i --max moraju biti brojevi.");
        }
    }

    private void handleStatus(Command command) {
        if (!command.hasArg("job")) {
            System.out.println("STATUS komanda zahteva: --job");
            return;
        }

        String jobId = command.getArg("job");

        executorService.submit(() -> {
            String status = jobManager.getStatus(jobId);
            System.out.println(jobId + " is " + status);
        });
    }

    private void handleMap() {
        executorService.submit(() -> {
            var snapshot = mapAggregator.snapshot();

            if (snapshot.isEmpty()) {
                System.out.println("[MAP] Mapa još uvek nije dostupna.");
                return;
            }

            StringBuilder builder = new StringBuilder();

            for (char firstLetter = 'a'; firstLetter <= 'z'; firstLetter += 2) {

                char secondLetter = (char)(firstLetter + 1);

                StationData data1 = snapshot.getOrDefault(firstLetter, new StationData());
                StationData data2 = snapshot.getOrDefault(secondLetter, new StationData());

                builder.append(firstLetter).append(": ")
                        .append(data1.count()).append(" - ").append(data1.sum())
                        .append(" | ")
                        .append(secondLetter).append(": ")
                        .append(data2.count()).append(" - ").append(data2.sum())
                        .append("\n");
            }

            System.out.print(builder);
        });
    }

    private void handleExportMap() {
        executorService.submit(() -> {
            var snapshot = mapAggregator.snapshot();

            if (snapshot.isEmpty()) {
                System.out.println("[EXPORTMAP] Mapa još uvek nije dostupna.");
                return;
            }

            Logger.exportSnapshot(snapshot);
            System.out.println("[EXPORTMAP] Mapa uspešno eksportovana u map_log.csv");
        });

    }

    public Map<String, Command> getActiveScanCommandsMap() {
        return new HashMap<>(activeScanCommands);
    }


    public void removeActiveScanCommand(String jobId) {
        activeScanCommands.remove(jobId);
    }
}
