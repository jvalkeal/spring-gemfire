package org.springframework.data.gemfire.function;

import java.util.Collection;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;

public class ValueSumFunction extends FunctionAdapter {

    @Override
    public void execute(FunctionContext fc) {
        RegionFunctionContext context = (RegionFunctionContext)fc;
        Region<String, Integer> dataSet = context.getDataSet();
        
        int sum = 0;
        Collection<Integer> values = dataSet.values();
        for (Integer integer : values) {
            sum += integer;
        }
        context.getResultSender().lastResult(sum);
    }

    @Override
    public String getId() {
        return "ValueSumFunction";
    }

}
