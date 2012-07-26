package org.springframework.data.gemfire.function;

import java.util.List;

import org.springframework.data.gemfire.repository.samplefunction.Measurement;
import org.springframework.data.gemfire.repository.samplefunction.Measurements;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;

public class AddMeasurementTicksFunction extends FunctionAdapter {

    private static final long serialVersionUID = -3670869536180624350L;

    @Override
    public void execute(FunctionContext fc) {
        RegionFunctionContext context = (RegionFunctionContext)fc;
        Object[] arguments = (Object[]) context.getArguments();
        Region<Object, Object> dataSet = context.getDataSet();
        
        String id = (String) arguments[0];
        List<Measurement> meas = (List<Measurement>) arguments[1];
        
        Measurements mea = (Measurements) dataSet.get(id);
        if(mea == null) {
            mea = new Measurements(id);
        }
        mea.addMeasurements(meas);
        
        dataSet.put(id, mea);
        
        fc.getResultSender().lastResult("done");
    }

    @Override
    public String getId() {
        return "AddMeasurementTicksFunction";
    }

}
