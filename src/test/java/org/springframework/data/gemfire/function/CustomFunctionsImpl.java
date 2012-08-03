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

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;

@GemfireFunctionService
public class CustomFunctionsImpl implements CustomFunctions {
    
    @Override
    @GemfireFunction(id="FunctionIdContextResultsVoid")
    public void functionIdContextResultsVoid(@GemfireFunctionContext FunctionContext ctx) {
        ctx.getResultSender().lastResult(1);
    }

    @Override
    @GemfireFunction
    @GemfireFunctionExecute
    public int functionExecuteContextResultsInt(@GemfireFunctionContext FunctionContext ctx) {
        ctx.getResultSender().sendResult(2);
        return 22;
    }

    @Override
    @GemfireFunctionExecute(id="functionExecuteContextResultsInt", target=FunctionTarget.ON_ALL_DS_MEMBERS, collector=MyArrayListResultCollector.class)
    public List<Integer> executeIdList() {
        return null;
    }

    @Override
    @GemfireFunction
    @GemfireFunctionExecute(target=FunctionTarget.ON_ALL_DS_MEMBERS)
    public Object functionExecuteResultsString() {
        return "DONE";
    }
    
    @Override
    @GemfireFunction
    @GemfireFunctionExecute(id="functionExecuteResultsEntrySet", target=FunctionTarget.ON_REGION, value="Measurements")
    public Object functionExecuteResultsEntrySet(@GemfireFunctionContext RegionFunctionContext ctx, @GemfireFunctionFilter Set<String> filter) {
        Region<Object, Object> localPrimaryData = PartitionRegionHelper.getLocalPrimaryData(ctx.getDataSet());
        Set<Entry<Object, Object>> entrySet = localPrimaryData.entrySet();
        return entrySet;
    }
    
    @Override
    @GemfireFunction(id="FunctionExecuteResultsParametersInt")
    @GemfireFunctionExecute(id="FunctionExecuteResultsParametersInt", target=FunctionTarget.ON_ALL_DS_MEMBERS)
    public Object functionExecuteResultsParametersInt(@GemfireFunctionArgs Integer args) {
        return args;
    }

    public int noInterfaceMethod() {
        return 0;
    }
    
}
