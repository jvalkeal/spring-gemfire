/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.data.gemfire.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetAddress;

import org.junit.Test;
import org.springframework.data.gemfire.RecreatingContextTest;

import com.gemstone.gemfire.cache.server.CacheServer;

/**
 * Integration test validating various cache server configurations of GemFire
 * through Spring.
 * 
 * @author Vinesh Prasanna M
 */
public class CacheServerIntegrationTest extends RecreatingContextTest {

	@Override
	protected String location() {
		return "org/springframework/data/gemfire/server/basic-cacheserver.xml";
	}

	@Test
	public void testBasicCacheServer() throws Exception {
		CacheServer cacheServer = ctx.getBean("basic-cacheserver",
				CacheServer.class);
		assertNotNull("basic-cacheserver bean not available", cacheServer);
		assertTrue("Cache server default groups not expected",
				cacheServer.getGroups().length == 0);
		assertTrue("Cache server not running", cacheServer.isRunning());
	}

	@Test
	public void testAdvancedCacheServer() throws Exception {
		CacheServer cacheServer = ctx.getBean("advanced-cacheserver",
				CacheServer.class);
		assertNotNull("advanced-cacheserver bean not available", cacheServer);

		String bindAddr = null;
		try {
			bindAddr = InetAddress.getByName("localhost").getHostName();
		} catch (Exception e) {
			fail("Unable to resolve local adress : " + e.getMessage());
		}

		assertEquals("Cache server bindAddress not as expected",
				cacheServer.getBindAddress(), bindAddr);
		assertEquals("Cache server port not as expected",
				cacheServer.getPort(), 40406);
		assertEquals("Cache server hostnameForClients not as expected",
				cacheServer.getHostnameForClients(), "localhost");
		assertEquals("Cache server loadPollInterval not as expected",
				cacheServer.getLoadPollInterval(), 2000);
		assertEquals("Cache server maxConnections not as expected",
				cacheServer.getMaxConnections(), 64);
		assertEquals("Cache server maxThreads not as expected",
				cacheServer.getMaxThreads(), 16);
		assertEquals("Cache server maximumMessageCount not as expected",
				cacheServer.getMaximumMessageCount(), 1000);
		assertEquals("Cache server maximumTimeBetweenPings not as expected",
				cacheServer.getMaximumTimeBetweenPings(), 30000);

		assertTrue("Cache server groups not expected",
				cacheServer.getGroups().length == 1);

		assertEquals("Cache server evictionPolicy not as expected", cacheServer
				.getClientSubscriptionConfig().getEvictionPolicy(), "entry");
		assertEquals("Cache server evictionCapacity not as expected",
				cacheServer.getClientSubscriptionConfig().getCapacity(), 1000);
		assertEquals("Cache server overFlowDirectory not as expected",
				cacheServer.getClientSubscriptionConfig()
						.getOverflowDirectory(),
				new File(System.getProperty("java.io.tmpdir"))
						.getAbsolutePath());

		assertFalse("Cache server not running", cacheServer.isRunning());
	}
}
