package cli;

import core.DirectoryMonitor;
import core.JobManager;
import core.MapAggregator;
import core.PeriodicReporter;
import model.Command;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class CliThread extends Thread {

    private final BlockingQueue<Command> commandsQueue;
    private final CommandProcessor commandProcessor;
    private final DirectoryMonitor directoryMonitor;
    private final PeriodicReporter periodicReporter;
    private final ExecutorService executorServiceCommand;
    private  final ExecutorService executorServiceMonitor;
    private final JobManager jobManager;
    private boolean isRunning = false;


    public CliThread(BlockingQueue<Command> commandsQueue, CommandProcessor commandProcessor, DirectoryMonitor directoryMonitor, PeriodicReporter periodicReporter, ExecutorService executorServiceCommand, ExecutorService executorServiceMonitor, JobManager jobManager) {
        this.commandsQueue = commandsQueue;
        this.commandProcessor = commandProcessor;
        this.directoryMonitor = directoryMonitor;
        this.periodicReporter = periodicReporter;
        this.executorServiceCommand = executorServiceCommand;
        this.executorServiceMonitor = executorServiceMonitor;
        this.jobManager = jobManager;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (!Thread.currentThread().isInterrupted()) {
            String line = scanner.nextLine();

            if(line.trim().isEmpty())
                continue;

            Command command = CommandParser.parse(line);
            if(command == null){
                System.out.println("Nepoznata komanda");
                continue;
            }
            String commandName = command.getName();

            if (commandName.equals("SHUTDOWN")) {
                handleShutdown(command);
                break;
            }

            if (commandName.equals("START")) {
                handleStart(command);
                continue;
            }
            
            try {
                commandsQueue.put(command);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("CLI nit je prekinuta");
            }
        }
    }

    private void handleStart(Command command) {
        if(!isRunning) {
            System.out.println("[CLI] START komanda pokrenuta.");

            if (command.hasArg("load-jobs") || command.hasArg("L")) {
                jobManager.loadPendingJobs("load_config.txt", commandsQueue);
            }
            commandProcessor.start();
            directoryMonitor.start();
            periodicReporter.start();
            isRunning = true;
        }
    }

    private void handleShutdown(Command command) {
        System.out.println("[CLI] Pokrećem PRISILNI SHUTDOWN...");

        if (command.hasArg("save-jobs") || command.hasArg("s")) {
            Map<String, Command> scanCommands = commandProcessor.getActiveScanCommandsMap();
            jobManager.saveJobsAsText(scanCommands, "load_config.txt");
        }

        try {
            directoryMonitor.interrupt();
            commandProcessor.interrupt();
            periodicReporter.shutdown();
            commandProcessor.interrupt();
            executorServiceCommand.shutdownNow();
            executorServiceMonitor.shutdownNow();
        } catch (Exception e) {
            System.out.println("[CLI] Greška pri gašenju executor-a.");
        }

        deleteRemainingTmpFiles();

        System.out.println("[CLI] PRISILNI SHUTDOWN završen. Sistem ugašen.");
    }
    private void deleteRemainingTmpFiles() {
        File[] tmpFiles = new File(".").listFiles((dir, name) -> name.startsWith("tmp_") && name.endsWith(".tmp"));

        if (tmpFiles != null) {
            for (File f : tmpFiles) {
                f.delete();
            }
        }
    }

}
