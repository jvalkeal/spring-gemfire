package org.springframework.data.gemfire.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.ForkUtil;
import org.springframework.data.gemfire.fork.FunctionAnnotationCacheServerProcess;
import org.springframework.data.gemfire.repository.samplefunction.Measurement;
import org.springframework.data.gemfire.repository.samplefunction.Measurements;
import org.springframework.data.gemfire.repository.samplefunction.MeasurementsRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.Pool;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ClientServerFunctionAnnotationTests {
    
    Region<?, ?> region;
    
    Pool pool;

    @Autowired
    MeasurementsRepository repository;

    @Autowired
    private ApplicationContext ctx;
    
    @BeforeClass
    public static void startUp() throws Exception {
        ForkUtil.cacheServer(FunctionAnnotationCacheServerProcess.class);

    }
    
    @Before
    public void setup() {
        region = ctx.getBean("Measurements", Region.class);
        pool = ctx.getBean("clientMeasurementsPool", Pool.class);
    }

    @Test
    public void testSomething() {
        addTaggedMeasurements();
        
        Measurements findOne = repository.findOne("meas1");       
        assertNotNull(findOne);
        
        Collection<Measurements> queryByTags1 = repository.queryByTags("tag1");
        assertNotNull(queryByTags1);
        assertThat(queryByTags1.size(), is(2));

        Collection<Integer> countFromPool1 = repository.countFromPool("Measurements");
        int count1 = 0;
        for (Integer integer : countFromPool1) {
            count1 += integer;
        }
        assertThat(count1, is(4));

        Collection<Integer> countFromPool3 = repository.countFromServer("Measurements");
        int count3 = 0;
        for (Integer integer : countFromPool3) {
            count3 += integer;
        }
        assertThat(count3, is(4));

        repository.deleteRegionEntriesFromPool("Measurements");
        Collection<Integer> countFromPool2 = repository.countFromPool("Measurements");
        
        // starting from -1 to make sure it works
        int count2 = -1;
        for (Integer integer : countFromPool2) {
            count2 += integer;
        }
        count2++;
        assertThat(count2, is(0));
       
    }
    
    @After
    public void stop() {
        region.getRegionService().close();
    }

    @AfterClass
    public static void cleanUp() {
        ForkUtil.sendSignal();
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
