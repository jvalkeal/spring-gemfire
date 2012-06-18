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
package org.springframework.data.gemfire.dlock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.repository.sampledlock.Tick;
import org.springframework.data.gemfire.repository.sampledlock.TickSerie;
import org.springframework.data.gemfire.repository.sampledlock.TickSerieRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for direct usage of GemfireDistributedLockService and wrapped
 * DistributedLockService inside of it.
 * 
 * @author Janne Valkealahti
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DeclarativeBeanUsageTests {
    
    @Autowired
    GemfireDistributedLockService gemfireDistributedLockService;

    @Autowired
    TickSerieRepository repository;
    
    @Test
    public void testThreadedUpdaterManualLocking() throws Exception {
        
        int serieCount = 10;
        int tickCount = 100;
     
        assertNotNull(repository);
        assertNotNull(gemfireDistributedLockService);
        
        // threads are appending data to ticks in tickserie. This is
        // a get/put operations so locking happens during this operation
        // to ensure consistency.
        Thread t1 = new Thread(newTickSerieUpdateRunnable(repository, gemfireDistributedLockService, 0, serieCount, 0, tickCount));
        Thread t2 = new Thread(newTickSerieUpdateRunnable(repository, gemfireDistributedLockService, 0, serieCount, 100, tickCount));
        
        t1.start();
        t2.start();
        
        // wait thread to finish before bailing out,
        // otherwise we may get cache closed exception.
        t1.join();
        t2.join();

        assertThat(repository.count(), is((long)serieCount));
        
        // check that threads did they were meant to do
        for (int i = 0; i < serieCount; i++) {
            TickSerie serie = repository.findOne("serie" + i);
            assertThat(serie.ticks.size(), is(tickCount*2));            
        }
        

    }
    
    private Runnable newTickSerieUpdateRunnable(final TickSerieRepository repository,
            final GemfireDistributedLockService lockService, final int serieStartId, final int serieCount,
            final int tickStartId, final int tickCount) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = serieStartId; i < (serieStartId+serieCount); i++) {
                    String id = "serie" + i;
                    
                    for (int j = tickStartId; j < (tickStartId+tickCount); j++) {
                        
                        if(lockService != null) {
                            lockService.lock("testService", "testLock");                            
                        }
                        
                        TickSerie serie = repository.findOne(id);
                        if(serie == null) {
                            serie = new TickSerie(id);
                        }
                        serie.addTick(new Tick(j, j));
                        repository.save(serie);
                        
                        if(lockService != null) {
                            lockService.unlock("testService", "testLock");                            
                        }
                    }
                    
                }                
            }
        };
        return runnable;
    }

    
    
}
