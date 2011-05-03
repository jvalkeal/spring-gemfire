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

package org.springframework.data.gemfire.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetAddress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean.EvictionPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.bp.edu.emory.mathcs.backport.java.util.Arrays;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.server.CacheServer;

/**
 * @author Vinesh Prasanna M.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("cacheserver-ns.xml")
public class CacheServerNamespaceTest {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testBasicCacheServer() throws Exception {
		final Cache cache = context.getBean("cache-with-xml", Cache.class);
		assertNotNull(cache);

		final CacheServerFactoryBean csb = (CacheServerFactoryBean) context
				.getBean("&gemfire-cacheserver");
		assertNotNull("cache server factory bean not available.", csb);

		assertTrue(context.containsBean("gemfire-cacheserver"));
		final CacheServer cacheServer = context.getBean("gemfire-cacheserver",
				CacheServer.class);

		assertTrue("Basic Cache Server not available.", cache.getCacheServers()
				.contains(cacheServer));
		assertTrue("Unexpected cache server port.",
				cacheServer.getPort() == 40404);

	}

	@Test
	public void testComplexCacheServer() throws Exception {
		assertTrue(context.containsBean("complexserver"));
		final CacheServer cacheServer = context.getBean("complexserver",
				CacheServer.class);
		final CacheServerFactoryBean csfb = (CacheServerFactoryBean) context
				.getBean("&complexserver");

		String bindAddr = null;
		try {
			bindAddr = InetAddress.getByName("localhost").getHostName();
		} catch (Exception e) {
			fail("Unable to resolve local adress : " + e.getMessage());
		}

		assertEquals("Cache server bindAddress not as expected",
				cacheServer.getBindAddress(), bindAddr);
		assertEquals("Cache server bindAddress not as expected",
				csfb.getBindAddress(), bindAddr);

		assertEquals("Cache server port not as expected",
				cacheServer.getPort(), 40406);
		assertEquals("Cache server port not as expected", csfb.getPort(), 40406);

		assertEquals("Cache server hostnameForClients not as expected",
				cacheServer.getHostnameForClients(), "localhost");
		assertEquals("Cache server hostnameForClients not as expected",
				csfb.getHostNameForClients(), "localhost");

		assertEquals("Cache server loadPollInterval not as expected",
				cacheServer.getLoadPollInterval(), 2000);
		assertEquals("Cache server loadPollInterval not as expected",
				csfb.getLoadPollInterval(), 2000);

		assertEquals("Cache server maxConnections not as expected",
				cacheServer.getMaxConnections(), 64);
		assertEquals("Cache server maxConnections not as expected",
				csfb.getMaxConnections(), 64);

		assertEquals("Cache server maxThreads not as expected",
				cacheServer.getMaxThreads(), 16);
		assertEquals("Cache server maxThreads not as expected",
				csfb.getMaxThreads(), 16);

		assertEquals("Cache server maximumMessageCount not as expected",
				cacheServer.getMaximumMessageCount(), 1000);
		assertEquals("Cache server maximumMessageCount not as expected",
				csfb.getMaxMessageCount(), 1000);

		assertEquals("Cache server maximumTimeBetweenPings not as expected",
				cacheServer.getMaximumTimeBetweenPings(), 30000);
		assertEquals("Cache server maximumTimeBetweenPings not as expected",
				csfb.getMaxTimeBetweenPings(), 30000);

		assertTrue("Cache server groups not expected",
				cacheServer.getGroups().length == 1);
		assertTrue("Cache server groups not expected",
				Arrays.asList(cacheServer.getGroups()).contains("DEFAULT"));
		assertTrue("Cache server groups not expected", csfb.getServerGroups()
				.size() == 1);
		assertTrue("Cache server groups not expected", csfb.getServerGroups()
				.contains("DEFAULT"));

		assertEquals("Cache server evictionPolicy not as expected", cacheServer
				.getClientSubscriptionConfig().getEvictionPolicy(), "entry");
		assertEquals("Cache server evictionPolicy not as expected",
				csfb.getEvictionPolicy(), EvictionPolicy.valueOf("entry"));

		assertEquals("Cache server evictionCapacity not as expected",
				cacheServer.getClientSubscriptionConfig().getCapacity(), 1000);
		assertEquals("Cache server evictionCapacity not as expected",
				csfb.getEvictionCapacity(), 1000);

		final File tmpFile = new File(System.getProperty("java.io.tmpdir"));
		assertEquals("Cache server overFlowDirectory not as expected",
				cacheServer.getClientSubscriptionConfig()
						.getOverflowDirectory(), tmpFile.getAbsolutePath());
		assertEquals("Cache server overFlowDirectory not as expected",
				csfb.getOverFlowDirectory(), tmpFile);

		assertFalse("Cache server not running", cacheServer.isRunning());
	}
}
