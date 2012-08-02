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
package org.springframework.data.gemfire.repository.samplefunction;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.gemfire.function.GemfireRepositoryFunction;
import org.springframework.data.gemfire.function.FunctionTarget;
import org.springframework.data.gemfire.function.MyArrayListResultCollector;
import org.springframework.data.repository.Repository;

/**
 * 
 * @author Janne Valkealahti
 */
public interface MeasurementsRepository extends Repository<Measurements, String>{

    Measurements save(Measurements entity);
    
    Measurements findOne(String id);
    
    Iterable<Measurements> findAll();

    long count();

    @GemfireRepositoryFunction(id="MeasurementsByTagsFunction", target=FunctionTarget.ON_REGION, value="Measurements", collector=MyArrayListResultCollector.class)
    Collection<Measurements> queryByTags(String... tags);

    @GemfireRepositoryFunction(id="MeasurementsByTagsFunction", collector=MyArrayListResultCollector.class, filter=2)
    Collection<Measurements> queryByTagsAndFilter(String[] tags, Set<String> filter);

    @GemfireRepositoryFunction(id="AddMeasurementTicksFunction")
    void addMeasurementTicks(String id, List<Measurement> meas);

    @GemfireRepositoryFunction(id="DeleteRegionEntriesFunction", target=FunctionTarget.ON_ALL_DS_MEMBERS)
    void deleteRegionEntries(String regionId);

    @GemfireRepositoryFunction(id="DeleteRegionEntriesFunction", target=FunctionTarget.ON_SERVER_POOL, value="clientMeasurementsPool")
    void deleteRegionEntriesFromPool(String regionId);

    @GemfireRepositoryFunction(id="RegionEntryCountFunction", target=FunctionTarget.ON_SERVER_POOL, value="clientMeasurementsPool")
    Collection<Integer> countFromPool(String regionId);

    @GemfireRepositoryFunction(id="RegionEntryCountFunction", target=FunctionTarget.ON_SERVER_CACHE)
    Collection<Integer> countFromServer(String regionId);

}