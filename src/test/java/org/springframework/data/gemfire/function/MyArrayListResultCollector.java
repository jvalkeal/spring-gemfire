package org.springframework.data.gemfire.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;

public class MyArrayListResultCollector implements ResultCollector<Serializable, Serializable> {

    final ArrayList<Serializable> result = new ArrayList<Serializable>();

    @Override
    public void addResult(DistributedMember memberID, Serializable resultOfSingleExecution) {
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
    public Serializable getResult() throws FunctionException {
        return this.result;
    }

    @Override
    public Serializable getResult(long timeout, TimeUnit unit) throws FunctionException, InterruptedException {
        return this.result;
    }

}
