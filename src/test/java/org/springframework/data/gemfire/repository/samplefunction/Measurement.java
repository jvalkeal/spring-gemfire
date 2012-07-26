package org.springframework.data.gemfire.repository.samplefunction;

public class Measurement {
    
    public long timestamp;
    public double value;
    public Measurement(long timestamp, double value) {
        super();
        this.timestamp = timestamp;
        this.value = value;
    }

}