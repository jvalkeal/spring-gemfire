/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.gemfire.ForkUtil;
import org.springframework.data.gemfire.fork.FunctionCacheServerProcess;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.client.PoolFactory;
import com.gemstone.gemfire.cache.client.PoolManager;

/**
 * @author David Turanski
 *
 */
public class GemfireFunctionTemplateTests {
	private static ClientCache cache = null;

	private static Pool pool = null;

	private static Region<String, Integer> clientRegion = null;

	@BeforeClass
	public static void startUp() throws Exception {
		ForkUtil.cacheServer(FunctionCacheServerProcess.class);

		Properties props = new Properties();
		props.put("mcast-port", "0");
		props.put("name", "function-client");
		props.put("log-level", "warning");

		ClientCacheFactory ccf = new ClientCacheFactory(props);
		ccf.setPoolSubscriptionEnabled(true);
		cache = ccf.create();

		PoolFactory pf = PoolManager.createFactory();
		pf.addServer("localhost", 40404);
		pf.setSubscriptionEnabled(true);
		pool = pf.create("client");

		ClientRegionFactory<String, Integer> crf = cache.createClientRegionFactory(ClientRegionShortcut.PROXY);
		crf.setPoolName("client");
		clientRegion = crf.create("test-function");
	}

	@AfterClass
	public static void cleanUp() {
		ForkUtil.sendSignal();
		if (clientRegion != null) {
			clientRegion.destroyRegion();
		}
		if (pool != null) {
			pool.destroy();
			pool = null;
		}

		if (cache != null) {
			cache.close();
		}
		cache = null;
	}
	
	@Test 
	public void testExecuteOnRegion() {
		GemfireFunctionOperations template = new GemfireFunctionTemplate(cache);
		List<Integer> sumList = template.executeOnRegion("ValueSumFunction", null, null, null, "test-function", -1);
		assertThat(sumList.size(), is(1));
	}
	
    @Test
    public void testExecuteOnRegionWithCollector() {
        GemfireFunctionOperations template = new GemfireFunctionTemplate(cache);
        IntegerSumResultCollector collector = new IntegerSumResultCollector();
        Integer sum = template.executeOnRegion("ValueSumFunction", null, collector, null, "test-function", -1);
        assertThat(sum, is(6));
    }

}
