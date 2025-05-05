package model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StationData {

    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicLong sum = new AtomicLong(0);

    public void addMeasurement(double value) {
        count.incrementAndGet();
        sum.addAndGet((long)(value * 1000));
    }

    public int count() {
        return count.get();
    }

    public double sum() {
        return sum.get() / 10.0;
    }
    public StationData copy() {
        StationData copy = new StationData();
        copy.count.set(this.count.get());
        copy.sum.set(this.sum.get());
        return copy;
    }

}
