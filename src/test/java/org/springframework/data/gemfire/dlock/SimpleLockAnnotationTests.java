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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.repository.sampledlock.Tick;
import org.springframework.data.gemfire.repository.sampledlock.TickSerie;
import org.springframework.data.gemfire.repository.sampledlock.TickSerieRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author Janne Valkealahti
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SimpleLockAnnotationTests {
    
    @Autowired
    TickSerieRepository repository;
    
    @Test
    public void testAnnotationLockWithThreading() throws Exception {
        
        assertNotNull(repository);
        
        // fire thread which does x number of iterations and in each iteration
        // y number of tick series is updated with one tick having value from
        // iteration counter.
        Thread t1 = new Thread(newTickSerieSetSameUpdated(repository, 0, 100, 100));
        t1.start();
        
        Collection<String> ids = new ArrayList<String>(100);
        for (int i = 0; i < 100; i++) {
            ids.add("serie" + i);
        }
                
        // check that all series have same tick values.
        // demonstrates locking of read/write on a repository
        // method level
        for (int i = 0; i < 100; i++) {
            Iterable<TickSerie> findAll = repository.findAll(ids);
            Iterator<TickSerie> iterator = findAll.iterator();
            Double checkedValue = null;
            while (iterator.hasNext()) {
                TickSerie tickSerie = (TickSerie) iterator.next();
                if(tickSerie == null) {
                    continue;
                }
                double val = tickSerie.ticks.get(0).value;
                if(checkedValue == null) {
                    checkedValue = new Double(val);
                } else {
                    assertThat(checkedValue, is(new Double(val)));
                }
                
            }
        }
        
        // wait thread to finish before bailing out,
        // otherwise we may get cache closed exception.
        t1.join();

    }
    
    private Runnable newTickSerieSetSameUpdated(final TickSerieRepository repository, final int serieStartId, final int serieCount, final int iterations) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < iterations; i++) {
                    ArrayList<TickSerie> series = new ArrayList<TickSerie>(serieCount);
                    for (int j = serieStartId; j < (serieStartId+serieCount); j++) {
                        String id = "serie" + j;
                        TickSerie serie = new TickSerie(id);
                        serie.addTick(new Tick(j, i));
                        series.add(serie);
                    }
                    repository.save(series);
                }
            }
        };
        return runnable;
    }
    
}
