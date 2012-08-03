package org.springframework.data.gemfire.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;

public class IntegerSumResultCollector implements ResultCollector<Integer, Integer> {

    final ArrayList<Integer> result = new ArrayList<Integer>();

    @Override
    public void addResult(DistributedMember memberID, Integer resultOfSingleExecution) {
        // no null results
        if(resultOfSingleExecution != null) {
            this.result.add(resultOfSingleExecution);            
        }
    }

    @Override
    public void clearResults() {
        result.clear();
    }

    @Override
    public void endResults() {
    }

    @Override
    public Integer getResult() throws FunctionException {
        int sum = 0;
        for (Integer integer : result) {
            sum += integer;
        }
        return sum;
    }

    @Override
    public Integer getResult(long timeout, TimeUnit unit) throws FunctionException, InterruptedException {
        return this.getResult();
    }

}
