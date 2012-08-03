package org.springframework.data.gemfire.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedSystem;

/**
 * Overall big picture tests by letting context to create
 * functions, function proxies, execution interceptors.
 * 
 * Making sure we can do "magic" function executions
 * without really seeing native function code.
 * 
 * @author Janne Valkealahti
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireFunctionServiceTests {

    @Autowired    
    ApplicationContext ctx;

    Region<String, String> region;    
    CustomFunctions customFunctions;    
    DistributedSystem ds;
    
    @Before
    public void setup() {
        region = (Region)ctx.getBean("Measurements");
        customFunctions = (CustomFunctions)ctx.getBean("customFunctionsExecuteIdList");
        ds = CacheFactory.getAnyInstance().getDistributedSystem();
        
        // just warm up region by adding some content
        region.put("key1", "value1");
        region.put("key2", "value2");
        region.put("key3", "value3");
    }
    
    @Test
    public void testFunctionsRegistered() {
        // from annotations
        assertNotNull(FunctionService.getFunction("FunctionIdContextResultsVoid"));
        assertNotNull(FunctionService.getFunction("functionExecuteContextResultsInt"));
        // from interface
        assertNotNull(FunctionService.getFunction("AddMeasurementTicksFunction"));
    }
    
    @Test
    public void testNativeFunctionExecution() {
        Execution onMembers = FunctionService.onMembers(ds);
        ResultCollector<?, ?> execute = onMembers.execute("FunctionIdContextResultsVoid");
        List<Integer> result = (List<Integer>) execute.getResult();
        assertNotNull(result);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(1));
    }
    
    @Test
    public void testInterceptedFunctionExecution1() {
        // 1. we call CustomFunctions.executeIdList()
        // 2. method call is intercepted
        // 3. CustomFunctionsImpl.functionIdContextResultsVoid(FunctionContext)
        //    executed as gemfire function.
        List<Integer> result = customFunctions.executeIdList();
        assertNotNull(result);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is(2));        
        assertThat(result.get(1), is(22));        
    }

    @Test
    public void testInterceptedFunctionExecution2() {
        // function returns Strings
        // function results has ArrayList of Strings
        // keeping type as object to avoid casting errors
        Object returned = customFunctions.functionExecuteResultsString();
        List<String> result = (List<String>) returned;
        assertNotNull(result);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is("DONE"));        
    }

    @Test
    public void testInterceptedFunctionExecution3() {
        Set<String> filter = new HashSet<String>();
        filter.add("routingkey");
        Object returned = customFunctions.functionExecuteResultsEntrySet(null, filter);
        List<Set<Entry<Object, Object>>> result = (List<Set<Entry<Object, Object>>>) returned;
        assertNotNull(result);
        assertThat(result.size(), is(1));

        Set<Entry<Object, Object>> set = result.get(0);
        assertThat(set.size(), is(3));

        // check that we have something like: {key1,value1}, {key2,value2}, {key3,value3}
        String check = "xxx";
        for (Entry<Object, Object> entry : set) {
            assertThat(check.equals(entry.getKey()), is(false));
            assertThat(((String)entry.getValue()).startsWith("value"), is(true));
            check = (String)entry.getKey();
        }
    }

    @Test
    public void testInterceptedFunctionExecution4() {
        Object returned = customFunctions.functionExecuteResultsParametersInt(123);
        List<Integer> result = (List<Integer>) returned;
        assertNotNull(result);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(123));        
    }

}
