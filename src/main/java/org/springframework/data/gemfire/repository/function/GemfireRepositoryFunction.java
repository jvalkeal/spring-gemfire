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
package org.springframework.data.gemfire.repository.function;

import java.io.Serializable;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.data.gemfire.function.FunctionTarget;
import org.springframework.data.gemfire.function.GemfireFunctionTemplate;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.execute.ResultCollector;

/**
 * 
 * @author Janne Valkealahti
 */
public class GemfireRepositoryFunction implements RepositoryQuery {
    
    private final GemfireFunctionMethod functionMethod;
    private final GemfireFunctionTemplate functionTemplate;
    
    public GemfireRepositoryFunction(GemfireFunctionMethod functionMethod, GemfireFunctionTemplate functionTemplate) {
        Assert.notNull(functionMethod);
        this.functionMethod = functionMethod;
        this.functionTemplate = functionTemplate;
    }

    @Override
    public Object execute(Object[] parameters) {

        String annotatedFunction = functionMethod.getFunctionId();
        FunctionTarget target = functionMethod.getFunctionTarget();
        String value = functionMethod.getFunctionValue();
        Class<?> collectorClazz = functionMethod.getFunctionCollector();
        int filterParameter = functionMethod.getFunctionFilter();
        long timeout = functionMethod.getFunctionTimeout();
        
        Set<?> filter = null;
        if(filterParameter > 0 && parameters.length >= filterParameter) {
            if(Set.class.isAssignableFrom(parameters[filterParameter-1].getClass())) {
                filter = (Set<?>)parameters[filterParameter-1];
            }            
        }
        
        @SuppressWarnings("unchecked")
        ResultCollector<? extends Serializable,? extends Serializable> collector = 
                (ResultCollector<? extends Serializable, ? extends Serializable>) BeanUtils.instantiate(collectorClazz);
        
        // get pool or region or whatever else needed for function target
        if(target == FunctionTarget.ON_REGION) {
            return functionTemplate.executeOnRegion(annotatedFunction, parameters, collector, filter, value, timeout);
        } else if(target == FunctionTarget.ON_SERVER_POOL) {
            return  functionTemplate.executeOnServerPool(annotatedFunction, parameters, collector, value, timeout);
        } else if(target == FunctionTarget.ON_SERVERS_POOL) {
            return  functionTemplate.executeOnServersPool(annotatedFunction, parameters, collector, value, timeout);
        } else if(target == FunctionTarget.ON_SERVER_CACHE) {
            return  functionTemplate.executeOnServerCache(annotatedFunction, parameters, collector, value, timeout);
        } else if(target == FunctionTarget.ON_SERVERS_CACHE) {
            return  functionTemplate.executeOnServersCache(annotatedFunction, parameters, collector, value, timeout);
        } else if(target == FunctionTarget.ON_ALL_DS_MEMBERS) {
            return  functionTemplate.executeOnMembers(annotatedFunction, parameters, collector, value, timeout);
        } else {
            return null;
        }
        
    }

    @Override
    public QueryMethod getQueryMethod() {
        return this.functionMethod;
    }

}
