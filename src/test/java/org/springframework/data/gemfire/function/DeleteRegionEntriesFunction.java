package org.springframework.data.gemfire.function;

import java.util.Iterator;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;

public class DeleteRegionEntriesFunction extends FunctionAdapter {

    private static final long serialVersionUID = -8375054063710206144L;

    @Override
    public void execute(FunctionContext fc) {
        Object[] arguments = (Object[]) fc.getArguments();
        
        Cache anyInstance = CacheFactory.getAnyInstance();
        Region<Object, Object> region = anyInstance.getRegion((String)arguments[0]);
        region.invalidateRegion();
        Iterator<Object> iterator = region.keySet().iterator();
        while(iterator.hasNext()) {
            region.destroy(iterator.next());
        }
        
        fc.getResultSender().lastResult("done");
    }

    @Override
    public String getId() {
        return "DeleteRegionEntriesFunction";
    }

}
