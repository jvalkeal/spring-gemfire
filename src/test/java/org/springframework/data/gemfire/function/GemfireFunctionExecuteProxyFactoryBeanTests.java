package org.springframework.data.gemfire.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireFunctionExecuteProxyFactoryBeanTests {

    @Autowired
    ApplicationContext ctx;
    
    @Test
    public void testExecuteIdList() throws Exception {
        
        CustomFunctions o = (CustomFunctions)ctx.getBean("customFunctionsExecuteIdList");
        assertNotNull(o);
        
    }
    
    
}
