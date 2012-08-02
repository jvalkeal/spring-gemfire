/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.springframework.data.gemfire.RecreatingContextTest;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.function.support.GemfireFunctionServiceFactoryBean;

public class FunctionServiceNamespaceTest extends RecreatingContextTest {

    @Override
    protected String location() {
        return "org/springframework/data/gemfire/config/function-ns.xml";
    }

    @Test
    public void testBasicUsage() throws Exception {
        assertTrue(ctx.containsBean("gemfire-function-service"));
        
        GemfireFunctionServiceFactoryBean fsfb = (GemfireFunctionServiceFactoryBean) ctx.getBean("&gemfire-function-service");
        assertNotNull(fsfb);
        assertEquals("org.springframework.data.gemfire", TestUtils.readField("basePackage", fsfb));
        assertEquals(true, TestUtils.readField("scanInterface", fsfb));
        assertEquals(true, TestUtils.readField("scanAnnotated", fsfb));
        
        List<String> functions = TestUtils.readField("functions", fsfb);
        assertThat(functions.size(), is(1));
        
    }
    
    @Test
    public void testEmpty() throws Exception {
        assertTrue(ctx.containsBean("gemfire-function-service-empty"));
        
        GemfireFunctionServiceFactoryBean fsfb = (GemfireFunctionServiceFactoryBean) ctx.getBean("&gemfire-function-service-empty");
        assertNotNull(fsfb);
        assertEquals(null, TestUtils.readField("basePackage", fsfb));
        assertEquals(false, TestUtils.readField("scanInterface", fsfb));
        assertEquals(false, TestUtils.readField("scanAnnotated", fsfb));
        
        List<String> functions = TestUtils.readField("functions", fsfb);
        assertNull(functions);
    }

    @Test
    public void testOnlyAnnotated() throws Exception {
        assertTrue(ctx.containsBean("gemfire-function-service-onlyannotated"));
        
        GemfireFunctionServiceFactoryBean fsfb = (GemfireFunctionServiceFactoryBean) ctx.getBean("&gemfire-function-service-onlyannotated");
        assertNotNull(fsfb);
        assertEquals("org.springframework.data.gemfire", TestUtils.readField("basePackage", fsfb));
        assertEquals(false, TestUtils.readField("scanInterface", fsfb));
        assertEquals(true, TestUtils.readField("scanAnnotated", fsfb));
        
        List<String> functions = TestUtils.readField("functions", fsfb);
        assertNull(functions);
    }
    
}
