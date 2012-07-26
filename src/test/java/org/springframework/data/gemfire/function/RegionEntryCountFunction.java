package org.springframework.data.gemfire.function;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;

public class RegionEntryCountFunction extends FunctionAdapter {

    private static final long serialVersionUID = 5877852048846911228L;

    @Override
    public void execute(FunctionContext fc) {
        Object[] arguments = (Object[]) fc.getArguments();
        
        Cache anyInstance = CacheFactory.getAnyInstance();
        Region<Object, Object> region = anyInstance.getRegion((String)arguments[0]);
        
        Region<Object, Object> localPrimaryData = PartitionRegionHelper.getLocalPrimaryData(region);
        
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        fc.getResultSender().lastResult(localPrimaryData.size());
    }

    @Override
    public String getId() {
        return "RegionEntryCountFunction";
    }    
    
}
