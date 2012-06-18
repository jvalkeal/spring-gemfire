/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.repository.sampledlock;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;

/**
 * Combination of ticks with is as String.
 * 
 * @author Janne Valkealahti
 */
public class TickSerie implements Serializable, DataSerializable {

    private static final long serialVersionUID = -8837226423956523957L;
    
    @Id
    public String id;
    public List<Tick> ticks;
    
    public TickSerie() {
        this.ticks = new ArrayList<Tick>();     
    }
    
    public TickSerie(String id) {
        super();
        this.id = id;
        this.ticks = new ArrayList<Tick>();
    }
    
    public String getId() {
        return id;
    }
    
    public void addTick(Tick tick) {
        ticks.add(tick);
    }

    @Override
    public void fromData(DataInput input) throws IOException, ClassNotFoundException {
        this.id = DataSerializer.readString(input);
        int tickCount = DataSerializer.readInteger(input);
        for (int i = 0; i < tickCount; i++) {
            addTick(new Tick(DataSerializer.readLong(input), DataSerializer.readDouble(input)));
        }
    }

    @Override
    public void toData(DataOutput output) throws IOException {
        DataSerializer.writeString(id, output);
        DataSerializer.writeInteger(ticks.size(), output);
        for (Tick tick : ticks) {
            DataSerializer.writeLong(tick.timestamp, output);        
            DataSerializer.writeDouble(tick.value, output);    
        }
        
    }
    
    @Override
    public String toString() {
        return "id=" + id + " / tickcount=" + ticks.size();
    }
    
}
