package org.springframework.data.gemfire.repository.samplefunction;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;

public class Measurements implements Serializable, DataSerializable {

    private static final long serialVersionUID = -8837226423956523957L;
    
    @Id
    public String id;
    public List<Measurement> measurements;
    public List<String> tags;
    
    public Measurements() {
        this.measurements = new ArrayList<Measurement>();     
        this.tags = new ArrayList<String>();
    }
    
    public Measurements(String id) {
        super();
        this.id = id;
        this.measurements = new ArrayList<Measurement>();
        this.tags = new ArrayList<String>();
    }
    
    public String getId() {
        return id;
    }
    
    public void addMeasurement(Measurement measurement) {
        measurements.add(measurement);
    }
    
    public void addMeasurements(List<Measurement> measurements) {
        this.measurements.addAll(measurements);
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void addTags(String... tags) {
        for (String tag : tags) {
            this.tags.add(tag);
        }
    }

    @Override
    public void fromData(DataInput input) throws IOException, ClassNotFoundException {
        this.id = DataSerializer.readString(input);
        int measurementCount = DataSerializer.readInteger(input);
        for (int i = 0; i < measurementCount; i++) {
            addMeasurement(new Measurement(DataSerializer.readLong(input), DataSerializer.readDouble(input)));
        }
        this.tags = DataSerializer.readArrayList(input);
    }

    @Override
    public void toData(DataOutput output) throws IOException {
        DataSerializer.writeString(id, output);
        DataSerializer.writeInteger(measurements.size(), output);
        for (Measurement measurement : measurements) {
            DataSerializer.writeLong(measurement.timestamp, output);        
            DataSerializer.writeDouble(measurement.value, output);    
        }
        DataSerializer.writeArrayList((ArrayList)this.tags, output);
    }
    
    @Override
    public String toString() {
        return "id=" + id + " / measurementcount=" + measurements.size();
    }
    
}