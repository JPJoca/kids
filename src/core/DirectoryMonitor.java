package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DirectoryMonitor extends  Thread {
    private final Path directoryPath;
    private final ExecutorService executorService;
    private final Map<String,Long> fileTimestamps;

    public DirectoryMonitor(String directory, ExecutorService executorService) {
        this.directoryPath = Paths.get(directory);
        this.executorService = executorService;
        this.fileTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    public void run() {
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
          directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
          System.out.println("[MONITOR] Pratim direktorijum: " + directoryPath.toAbsolutePath());

          try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.{txt,csv}")) {
              for (Path file : stream) {
                  File f = file.toFile();
                  String fileName = f.getAbsolutePath();
                  long lastModified = f.lastModified();

                  if (!fileTimestamps.containsKey(fileName) || fileTimestamps.get(fileName) != lastModified) {
                      fileTimestamps.put(fileName, lastModified);
                      System.out.println("[MONITOR] Inicijalna obrada fajla: " + f.getName());
                      executorService.submit(new FileProcessor(file));
                  }
              }
          } catch (IOException e) {
              System.out.println("[MONITOR] Greška pri inicijalnom učitavanju fajlova.");
          }


          while (!Thread.currentThread().isInterrupted()) {
              WatchKey key = watchService.poll();

              if (key == null) {
                  try {
                      Thread.sleep(100);
                  } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                  }
                  continue;
              }

              for (WatchEvent<?> event : key.pollEvents()) {
                  Path changedDirectory = directoryPath.resolve((Path) event.context());
                  File file = changedDirectory.toFile();

                  if(file.getName().endsWith(".txt") || file.getName().endsWith(".csv")) {
                      long lastModified = file.lastModified();
                      String fileName = file.getAbsolutePath();

                      boolean isNew = !fileTimestamps.containsKey(fileName);
                      boolean isModified = fileTimestamps.get(fileName) == null || fileTimestamps.get(fileName) != lastModified;

                      if (isNew || isModified) {
                          fileTimestamps.put(fileName, lastModified);
                          MapAggregator.getInstance().clear();
                          if (isNew) {
                              System.out.println("[MONITOR] Novi fajl detektovan: " + file.getName());
                          } else {
                              System.out.println("[MONITOR] Izmenjen fajl: " + file.getName());
                          }

                          executorService.submit(new FileProcessor(file.toPath()));
                      }
                  }
              }
              key.reset();
          }

      } catch (IOException e) {
          System.out.println("[MONITOR] Greška u monitoringu direktorijuma. Nastavljam rad.");
      }
    }
}
