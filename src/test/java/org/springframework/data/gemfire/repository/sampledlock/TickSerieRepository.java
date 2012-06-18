/*
 * Copyright 2011 the original author or authors.
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

import org.springframework.data.gemfire.dlock.DistibutedLock;
import org.springframework.data.gemfire.dlock.DistributedLockPolicy;
import org.springframework.data.repository.Repository;

/**
 * Testing repository for TickSerie.
 * 
 * @author Janne Valkealahti
 */
public interface TickSerieRepository extends Repository<TickSerie, String>{

    TickSerie save(TickSerie entity);
    
    TickSerie findOne(String id);
    
    @DistibutedLock(serviceName="testService", policy=DistributedLockPolicy.CUSTOM, lockBy="testLock")
    Iterable<TickSerie> save(Iterable<TickSerie> entities);
    
    @DistibutedLock(serviceName="testService", policy=DistributedLockPolicy.CUSTOM, lockBy="testLock")
    Iterable<TickSerie> findAll(Iterable<String> ids);

    long count();
}
