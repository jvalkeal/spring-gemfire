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
package org.springframework.data.gemfire.function.support;

import java.util.List;
import java.util.Map;

import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionService;

/**
 * Bean handling function registration.
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionService {

    private List<Function> functions;
    
    /**
     * Default constructor.
     */
    public GemfireFunctionService() {
        super();
    }

    /**
     * Gets a list of function classes.
     * 
     * @return the functions
     */
    public List<Function> getFunctions() {
        return functions;
    }

    /**
     * Sets functions to be registered by this service.
     * 
     * @param functions the functions to set
     */
    public void setFunctions(List<Function> functions) {
        this.functions = functions;
    }

    /**
     * Registeres all know function classes to gemfire
     * function service.
     */
    public void registerFunctions() {
        for (Function function: functions) {
            FunctionService.registerFunction(function);
        }
    }
    
    /**
     * Gets registered functions.
     * 
     * @return Map of registered functions.
     */
    public Map<String, Function> getRegisteredFunctions() {
        return FunctionService.getRegisteredFunctions();
    }
}
