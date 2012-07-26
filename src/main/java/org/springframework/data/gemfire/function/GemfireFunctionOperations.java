/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.function;

import java.io.Serializable;
import java.util.Set;

import com.gemstone.gemfire.cache.execute.ResultCollector;

/**
 * @author David Turanski
 *
 * @param <T>
 */
public interface GemfireFunctionOperations {

    public <T> T executeOnRegion(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, Set<?> filter, String value,
            long timeout);

    public <T> T executeOnMembers(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, String value, long timeout);

    public <T> T executeOnServersPool(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, String value, long timeout);

    public <T> T executeOnServerPool(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, String value, long timeout);

    public <T> T executeOnServersCache(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, String value, long timeout);

    public <T> T executeOnServerCache(String functionId, Object[] parameters,
            ResultCollector<? extends Serializable, ? extends Serializable> collector, String value, long timeout);

}