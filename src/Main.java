import cli.CliThread;
import cli.CommandProcessor;
import core.*;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {



        String dataDirectory = "data";
        int threadPoolSize = 4;


        BlockingQueue<model.Command> commandQueue = new LinkedBlockingQueue<>();

        ExecutorService executorServiceCommand = Executors.newFixedThreadPool(threadPoolSize);
        ExecutorService executorServiceMonitor = Executors.newFixedThreadPool(threadPoolSize);

        JobManager jobManager = new JobManager();

        MapAggregator mapAggregator = MapAggregator.getInstance();

        DirectoryMonitor directoryMonitor = new DirectoryMonitor(dataDirectory, executorServiceMonitor);

        PeriodicReporter periodicReporter = new PeriodicReporter(mapAggregator);


        CommandProcessor commandProcessor = new CommandProcessor(
                commandQueue, jobManager, mapAggregator, executorServiceCommand, dataDirectory);


        CliThread cliThread = new CliThread
                (commandQueue,commandProcessor, directoryMonitor, periodicReporter, executorServiceCommand, executorServiceMonitor, jobManager);


        cliThread.start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[INFO] Zatvaranje sistema...");
            commandProcessor.interrupt();
            directoryMonitor.interrupt();
            periodicReporter.shutdown();
            cliThread.interrupt();

            executorServiceMonitor.shutdown();
            executorServiceCommand.shutdown();
        }));
    }

}
