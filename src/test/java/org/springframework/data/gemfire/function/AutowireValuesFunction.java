package org.springframework.data.gemfire.function;

import org.springframework.beans.factory.annotation.Value;

import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;

public class AutowireValuesFunction extends FunctionAdapter {

    private static final long serialVersionUID = 574431513751479144L;

    // should exist in context, injected
    @Value("${fakekey1:fakevalue1default}")
    private String fakekey1;

    // should not exist in context, using default
    @Value("${fakekey2:fakevalue2default}")
    private String fakekey2;
    
    @Override
    public void execute(FunctionContext fc) {
        fc.getResultSender().lastResult(fakekey1 + "-" + fakekey2);
    }

    @Override
    public String getId() {
        return "AutowireValuesFunction";
    }

}
