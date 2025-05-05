package core;

import model.Command;
import model.JobStatus;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {

    private final Map<String, JobStatus> jobStatusMap = new ConcurrentHashMap<>();


    public void registerJob(String jobId) {
        jobStatusMap.put(jobId, JobStatus.PENDING);
    }

    public boolean isJobRegistered(String jobId) {
        boolean exists = jobStatusMap.containsKey(jobId);

        if (exists) {
            System.out.println("[SCAN] Job " + jobId + " vec postoji.");
        }

        return exists;
    }

    public void updateStatus(String jobId, String status) {
        try {
            JobStatus newStatus = JobStatus.valueOf(status.toUpperCase());
            if (jobStatusMap.containsKey(jobId)) {
                jobStatusMap.put(jobId, newStatus);
            } else {
                System.out.println("[JOB] Greska: posao '" + jobId + "' nije registrovan.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[JOB] Greška: nepoznat status '" + status + "'");
        }
    }


    public String getStatus(String jobId) {
        JobStatus status = jobStatusMap.get(jobId);
        return status != null ? status.name().toLowerCase() : "not found";
    }



    public void saveJobsAsText(Map<String, Command> scanCommands, String filePath) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, Command> entry : scanCommands.entrySet()) {
                String jobId = entry.getKey();
                Command cmd = entry.getValue();
                String status = getStatus(jobId);


                out.println(jobId + ", " + status.toLowerCase());


                StringBuilder line = new StringBuilder("SCAN");
                for (var arg : cmd.getAllArgs().entrySet()) {
                    line.append(" --").append(arg.getKey()).append(" ").append(arg.getValue());
                }
                out.println(line);
            }
            System.out.println("[JOBMANAGER] Poslovi sa statusima i SCAN komande sačuvani u " + filePath);
        } catch (IOException e) {
            System.out.println("[JOBMANAGER] Greška pri čuvanju u fajl: " + filePath);
        }
    }

    public void loadPendingJobs(String filePath, BlockingQueue<Command> queue) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<Command> toSchedule = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String jobId = parts[0].trim();

                String scanLine = reader.readLine();
                if (scanLine == null || !scanLine.startsWith("SCAN")) {
                    System.out.println("[LOAD] Preskačem jer nema SCAN komande za: " + jobId);
                    continue;
                }

                Command cmd = cli.CommandParser.parse(scanLine);

                if (cmd != null && cmd.hasArg("job") && cmd.getArg("job").equals(jobId)) {
                    System.out.println("[LOAD] Učitan posao: " + jobId);

                    toSchedule.add(cmd);
                }
            }


            for (Command cmd : toSchedule) {
                queue.put(cmd);
            }

        } catch (Exception e) {
            System.out.println("[LOAD] Greška pri učitavanju poslova: " + filePath);
        }
    }

}


