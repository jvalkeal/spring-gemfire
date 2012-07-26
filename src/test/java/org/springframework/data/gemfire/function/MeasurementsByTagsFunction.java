package org.springframework.data.gemfire.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.data.gemfire.repository.samplefunction.Measurements;

import com.gemstone.bp.edu.emory.mathcs.backport.java.util.Arrays;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;

public class MeasurementsByTagsFunction extends FunctionAdapter {

    /**
     * 
     */
    private static final long serialVersionUID = 1269245650373158976L;

    @Override
    public void execute(FunctionContext fc) {
        FunctionContext context = (FunctionContext)fc;
        
        RegionFunctionContext rfc = (RegionFunctionContext)fc;
        Region<Object, Object> dataSet = rfc.getDataSet();
        
        Object argumentsObject = rfc.getArguments();
        Object[] arguments = ((Object[])argumentsObject);
        String[] tags = (String[])arguments[0];
        
        Iterator<Object> iterator = dataSet.values().iterator();

        boolean lastResult = false;
        while (iterator.hasNext()) {
            Object object = (Object) iterator.next();
            
            Measurements meas = (Measurements)object;

            List intersection = intersection(Arrays.asList(tags), meas.getTags());
            if (intersection.size() > 0) {
                if (iterator.hasNext()) {
                    context.getResultSender().sendResult((Serializable) object);
                } else {
                    lastResult = true;
                    context.getResultSender().lastResult((Serializable) object);
                }
            }
        }
        
        if(!lastResult) {
            context.getResultSender().lastResult(null);
        }
    }

    @Override
    public String getId() {
        return "MeasurementsByTagsFunction";
    }
    
    private <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();
        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

}
