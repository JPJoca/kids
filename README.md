# Distributed File Monitoring and Processing System

## Overview

This Java-based project implements a system that continuously monitors a specified directory for meteorological data files (`.txt` and `.csv`) and processes them using a multi-threaded approach. The goal is to filter and aggregate temperature measurements by meteorological station names, with support for efficient processing of large files.

---

## Features

-  **Directory Monitoring**  
  A custom `DirectoryMonitor` watches for new or modified `.txt` and `.csv` files in a target directory using Java's `WatchService` API.

-  **Parallel File Processing**  
  When changes are detected, the system uses an `ExecutorService` with 4 threads to process each file in parallel. Each file is handled by a separate `FileScanTask`.

-  **Data Filtering**  
  Each file line is parsed as `STATION_NAME;TEMPERATURE`. Records are filtered based on:
    - First letter of the station name
    - Temperature range (`min` to `max`)
    - `.csv` files automatically skip the header line

-  **In-Memory Aggregation**  
  Filtered data is stored in memory using a concurrent map, grouped by the first letter of station names. For each letter, the system tracks:
    - The number of matching stations
    - The sum of their temperature measurements

-  **Thread-Safe Execution**  
  All operations involving shared data structures are protected using concurrency-safe classes (`ConcurrentHashMap`, `AtomicInteger`, `AtomicLong`). The processing supports interruption.

-  **Temporary File Merging**  
  Each thread writes filtered results to a temporary `.tmp` file. After all threads finish, results are merged into a single final output file, and temporary files are deleted.

---

## Notes

This project was developed as part of a university course assignment in Concurrent and Distributed Systems (KiDS).  
It demonstrates the use of Java's multithreading and file monitoring APIs, along with safe concurrent data aggregation and filtering techniques. 