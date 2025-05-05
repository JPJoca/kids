package core;

import util.Logger;

import java.io.FileWriter;
import java.io.PrintWriter;

public class PeriodicReporter extends Thread {

    private final MapAggregator mapAggregator;
    private volatile boolean running = true;

    public PeriodicReporter(MapAggregator mapAggregator) {
        this.mapAggregator = mapAggregator;
    }

    @Override
    public void run() {
        System.out.println("[REPORTER] Periodic reporter pokrenut.");

        while (running) {
            try {
                Thread.sleep(60_000);
                exportSnapshot();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[REPORTER] Periodic reporter ugašen.");
    }

    private void exportSnapshot() {
        var snapshot = mapAggregator.snapshot();

        if (snapshot.isEmpty()) {
            System.out.println("[REPORTER] Mapa još uvek nije dostupna.");
            return;
        }

        Logger.exportSnapshot(snapshot);

        System.out.println("[REPORTER] Mapa uspešno eksportovana.");

    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}

