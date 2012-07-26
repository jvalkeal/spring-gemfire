/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.gemfire.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.repository.samplefunction.Measurement;
import org.springframework.data.gemfire.repository.samplefunction.Measurements;
import org.springframework.data.gemfire.repository.samplefunction.MeasurementsRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.execute.FunctionService;

/**
 * 
 * @author Janne Valkealahti
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SingleNodeFunctionAnnotationTests {

    @Autowired
    MeasurementsRepository repository;
    
    @BeforeClass
    public static void startUp() throws Exception {
        FunctionService.registerFunction(new MeasurementsByTagsFunction());
        FunctionService.registerFunction(new AddMeasurementTicksFunction());
        FunctionService.registerFunction(new DeleteRegionEntriesFunction());
    }
    
    @Test
    public void queryRegion() throws Exception {

        addTaggedMeasurements();
        
        Measurements findOne = repository.findOne("meas1");       
        assertNotNull(findOne);

        Collection<Measurements> queryByTags1 = repository.queryByTags("tag1");
        assertNotNull(queryByTags1);
        assertThat(queryByTags1.size(), is(2));

        Collection<Measurements> queryByTags2 = repository.queryByTags("tag1");
        assertNotNull(queryByTags2);
        assertThat(queryByTags2.size(), is(2));

        Collection<Measurements> queryByTags12 = repository.queryByTags("tag1", "tag2");
        assertNotNull(queryByTags12);
        assertThat(queryByTags12.size(), is(3));

        Collection<Measurements> queryByTags0 = repository.queryByTags("tag0");
        assertNotNull(queryByTags0);
        assertThat(queryByTags0.size(), is(0));

        // this test is a bit useless. single node -> routing always that
        Set<String> filter = new HashSet<String>();
        filter.add("meas1");
        Collection<Measurements> queryByTagsAndFilter = repository.queryByTagsAndFilter(new String[]{"tag0", "tag2"}, filter);
        assertNotNull(queryByTagsAndFilter);
        assertThat(queryByTagsAndFilter.size(), is(2));

        long count = repository.count();        
        assertThat(count, is(4l));

        List<Measurement> meas5list = new ArrayList<Measurement>();
        meas5list.add(new Measurement(1, 31.0));
        meas5list.add(new Measurement(2, 32.0));
        meas5list.add(new Measurement(3, 33.0));
        repository.addMeasurementTicks("meas5", meas5list);

        findOne = repository.findOne("meas5");       
        assertNotNull(findOne);
        assertThat(findOne.measurements.size(), is(3));
        
        count = repository.count();        
        assertThat(count, is(5l));
        
        repository.deleteRegionEntries("Measurements");
        count = repository.count();        
        assertThat(count, is(0l));
        
    }
    
    private void addTaggedMeasurements() {
        Measurements meas1 = new Measurements("meas1");
        meas1.addTags("tag1", "tag2");
        meas1.addMeasurement(new Measurement(1, 1.0));
        meas1.addMeasurement(new Measurement(2, 2.0));
        meas1.addMeasurement(new Measurement(3, 3.0));
        
        Measurements meas2 = new Measurements("meas2");
        meas2.addTags("tag1");
        meas2.addMeasurement(new Measurement(1, 11.0));
        meas2.addMeasurement(new Measurement(2, 12.0));
        meas2.addMeasurement(new Measurement(3, 13.0));
        
        Measurements meas3 = new Measurements("meas3");
        meas3.addTags("tag2");
        meas3.addMeasurement(new Measurement(1, 21.0));
        meas3.addMeasurement(new Measurement(2, 22.0));
        meas3.addMeasurement(new Measurement(3, 23.0));

        Measurements meas4 = new Measurements("meas4");
        meas4.addTags("tag3");
        meas4.addMeasurement(new Measurement(1, 21.0));
        meas4.addMeasurement(new Measurement(2, 22.0));
        meas4.addMeasurement(new Measurement(3, 23.0));

        repository.save(meas1);
        repository.save(meas2);
        repository.save(meas3);
        repository.save(meas4);
    }

}
