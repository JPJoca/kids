package core;

import model.StationData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapAggregator {

    private static final MapAggregator instance = new MapAggregator();

    private final Map<Character, StationData> dataMap;
    private final ReentrantReadWriteLock lock;

    public MapAggregator() {
        this.dataMap = new ConcurrentHashMap<>() {};
        this.lock = new ReentrantReadWriteLock();
    }

    public static MapAggregator getInstance() {
        return instance;
    }


    public void update(String stationName, double temperature) {
        if (stationName == null || stationName.isEmpty()) return;

        char firstLetter = Character.toLowerCase(stationName.charAt(0));

        lock.writeLock().lock();
        try {
            dataMap.computeIfAbsent(firstLetter, key -> new StationData())
                    .addMeasurement(temperature);
        } finally {
            lock.writeLock().unlock();
        }

    }


    public Map<Character, StationData> snapshot() {
        lock.readLock().lock();
        try {
            Map<Character, StationData> snapshot = new ConcurrentHashMap<>();
            for (Map.Entry<Character, StationData> entry : dataMap.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().copy());
            }
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }

    }


    public void clear() {
        dataMap.clear();
    }


    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return dataMap.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}

