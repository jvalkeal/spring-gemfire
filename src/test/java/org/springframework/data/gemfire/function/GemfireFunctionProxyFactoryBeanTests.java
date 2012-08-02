package org.springframework.data.gemfire.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.execute.Function;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireFunctionProxyFactoryBeanTests {

    @Autowired
    ApplicationContext ctx;
    
    @Test
    public void testFunctionIdContextResultsVoid() throws Exception {
        Function f = (Function)ctx.getBean("customFunctionsProxyFunctionIdContextResultsVoid");
        assertNotNull(f);
        assertThat(f.getId(), is("FunctionIdContextResultsVoid"));
        assertThat(f.isHA(), is(true));
        assertThat(f.hasResult(), is(true));
        assertThat(f.optimizeForWrite(), is(false));
    }

    @Test
    public void testFunctionExecuteContextResultsInt() throws Exception {
        Function f = (Function)ctx.getBean("customFunctionsProxyFunctionExecuteContextResultsInt");
        assertNotNull(f);
        assertThat(f.getId(), is("FunctionExecuteContextResultsInt"));
        assertThat(f.isHA(), is(false));
        assertThat(f.hasResult(), is(false));
        assertThat(f.optimizeForWrite(), is(false));
    }
        
}
