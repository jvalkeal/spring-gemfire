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

import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_BIND_ADDRESS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_GROUPS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_HOSTNAME_FOR_CLIENTS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_LOAD_POLL_INTERVAL;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_LOAD_PROBE;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_MAXIMUM_MESSAGE_COUNT;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_MAX_CONNECTIONS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_MAX_THREADS;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_MESSAGE_TIME_TO_LIVE;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_NOTIFY_BY_SUBSCRIPTION;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_PORT;
import static com.gemstone.gemfire.cache.server.CacheServer.DEFAULT_SOCKET_BUFFER_SIZE;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.InterestRegistrationListener;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.cache.server.ClientSubscriptionConfig;
import com.gemstone.gemfire.cache.server.ServerLoadProbe;

/**
 * A factory bean that configures a Gemfire <code>CacheServer</code> instance
 * for external access by Gemfire Clients. The cache server is set-up using the
 * configured <code>Cache</code>. If a cache is not available then a default
 * cache is looked up in the VM and used. This class is expected to be
 * configured by one thread, the behavior is undetermined when configured by
 * multiple threads.
 * 
 * @author vinesh
 * @since 1.1
 * 
 */
public final class CacheServerFactoryBean implements FactoryBean<CacheServer>,
		InitializingBean, DisposableBean, BeanNameAware {
	private static final Log log = LogFactory
			.getLog(CacheServerFactoryBean.class);

	/**
	 * flag controls if the created cache server should be started up
	 * immediately after it is configured.
	 */
	private boolean autoStart = true;

	/**
	 * The port on which a <Code>CacheServer</code> is configured to serve.
	 * Default port 40404.
	 */
	public int port = DEFAULT_PORT;

	/**
	 * The maximum number of client sockets accepted by a CacheServer. Default
	 * value: 800
	 * 
	 */
	public int maxConnections = DEFAULT_MAX_CONNECTIONS;

	/**
	 * The maximum number of cache server threads that can be created to service
	 * client requests. Once this number of threads exist then connections are
	 * multiplexed over available threads to service their request. The value of
	 * <code>0</code> causes a thread to be bound to every connection and to be
	 * dedicated to detecting client requests on that connection. Default value:
	 * 0
	 */
	public int maxThreads = DEFAULT_MAX_THREADS;

	/**
	 * The notify-by-subscription value of the <Code>CacheServer</code> which
	 * controls whether or not to notify clients based on key subscription.
	 */
	public boolean notifyBySubscription = DEFAULT_NOTIFY_BY_SUBSCRIPTION;

	/**
	 * The size of the TCP socket buffers for communication from the cache
	 * server to the client.
	 */
	public int socketBufferSize = DEFAULT_SOCKET_BUFFER_SIZE;

	/**
	 * The maximum amount of time (milliseconds) between client pings. This
	 * value is used to determine the health of this <code>CacheServer</code>'s
	 * clients. Default value : 60000 (1 minute)
	 */
	public int maxTimeBetweenPings = DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS;

	/**
	 * The maximum number of messages that can be enqueued in a client-queue. In
	 * the absence of overflow all cache operations that result in a client
	 * event are blocked once the queue hits this message count. Default value :
	 * 230000.
	 */
	public int maxMessageCount = DEFAULT_MAXIMUM_MESSAGE_COUNT;

	/**
	 * The default time (in seconds ) after which a message in the client queue
	 * will expire. Default value : 180
	 */
	public int messageTimeToLive = DEFAULT_MESSAGE_TIME_TO_LIVE;

	/**
	 * The list of server groups a cache server belongs to. The default is an
	 * empty list which indicates that the server is not a member of any
	 * specific group.
	 */
	public List<String> serverGroups = new ArrayList<String>(
			Arrays.asList(DEFAULT_GROUPS));

	/**
	 * The default load balancing probe. The default load balancing probe
	 * reports the connections counts of this cache server.
	 */
	public ServerLoadProbe serverLoadProbe = DEFAULT_LOAD_PROBE;

	/**
	 * The frequency (milliseconds) at which to poll the load probe for the load
	 * on this cache server. Default value: 5000 (5 seconds).
	 */
	public long loadPollInterval = DEFAULT_LOAD_POLL_INTERVAL;

	/**
	 * The ip address or host name that the cache server's socket will listen on
	 * for client connections. The default is an empty string which implied that
	 * the cache server will bind on the default address which is platform
	 * specific.
	 */
	public String bindAddress = DEFAULT_BIND_ADDRESS;

	/**
	 * The ip address or host name that will be provided to clients as the host
	 * this cache server is listening on. This is helpful in NAT'ed scenarios
	 * where in the internal bind address the cache server is listening on is
	 * different to the externally resolve address for in-bound client
	 * connections.The default is an empty string which implies this is platform
	 * specific default.
	 */
	public String hostNameForClients = DEFAULT_HOSTNAME_FOR_CLIENTS;

	/**
	 * An enumeration of possible eviction policy for cache server client
	 * queues.
	 * 
	 * @author Vinesh Prasanna M
	 * 
	 */
	public static enum EvictionPolicy {
		/**
		 * No eviction (default)
		 * 
		 * @see <code>ClientSubscriptionConfig.DEFAULT_EVICTION_POLICY</code>
		 */
		none,
		/**
		 * An eviction that is governed by the entry count in the client queue.
		 * 
		 * @see <code>ClientSubscriptionConfig.DEFAULT_EVICTION_POLICY</code>
		 */
		entry,

		/**
		 * Client queue eviction based on the heap memory estimate of the client
		 * queue.
		 * 
		 * @see <code>ClientSubscriptionConfig.DEFAULT_EVICTION_POLICY</code>
		 */
		mem
	}

	/**
	 * Eviction policy for client notification queue's. Default value : none
	 */
	private EvictionPolicy evictionPolicy = EvictionPolicy
			.valueOf(ClientSubscriptionConfig.DEFAULT_EVICTION_POLICY);

	/**
	 * Eviction capacity to limit client notification queue for eviction. The
	 * capacity is interpreted as entryCount when the evictionPolicy is of type
	 * entry and memory (MB) when eviction policy is of type mem. This has no
	 * effect when evictionPolicy is none. Default value : 1.
	 */
	private int evictionCapacity = ClientSubscriptionConfig.DEFAULT_CAPACITY;

	/**
	 * The configured diskStore that is to be used for the overflow files. This
	 * property is valid only for GemFire 6.5+. Use of diskStore is mutually
	 * exclusive to overFlowDirectory in GemFire 6.5.x. Any attempt to use both
	 * will result in an <code>IllegalStateException</code> in GemFire 6.5.x.
	 * This property is ignored in GemFire versions prior to 6.5.
	 */
	private String diskStoreName;

	/**
	 * The directory (path) that is to be used for the overflow files. This is
	 * used for GemFire 6.0.x and prior to 6.5. Default value '.' i.e. Java
	 * ${user.dir}. Use of overflowDirectory is mutually exclusive with
	 * diskStore, attempt to use both in GemFire 6.5.x will result in an
	 * <code>IllegalStateException</code>.
	 */
	private File overFlowDirectory = new File(
			ClientSubscriptionConfig.DEFAULT_OVERFLOW_DIRECTORY);

	/**
	 * Collection of unique <code>InterestRegistrationListener</code>'s on this
	 * server.
	 */
	private Set<InterestRegistrationListener> listeners = new LinkedHashSet<InterestRegistrationListener>();

	/**
	 * The GemFire cache that is used to set-up the <code>CacheServer</code>.
	 */
	private Cache cache;
	private CacheServer cacheServer = null;

	private String beanName = null;

	private AtomicBoolean configured = new AtomicBoolean(false);

	public Class<?> getObjectType() {
		return (this.cacheServer != null) ? cacheServer.getClass()
				: CacheServer.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public CacheServer getObject() throws Exception {
		return this.cacheServer;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.cache == null || cache.isClosed()) {
			log.debug("GemfireCache not available, using default cache.");
			this.cache = CacheFactory.getAnyInstance();
		}
		log.debug("Using cache instance : " + this.cache);

		// configure the cache server.
		this.build();

		log.debug("Auto Starting Server : " + isAutoStart());
		// go ahead and start the server only if configured to auto start.
		if (this.isAutoStart()) {
			this.init();
		}
	}

	private void build() {
		assert (this.cache != null) : "Invalid state, cache not available";

		this.cacheServer = this.cache.addCacheServer();

		assert (this.cacheServer != null) : "Invalid state, cache server not available";

		this.cacheServer.setBindAddress(this.getBindAddress());
		this.cacheServer.setGroups(this.serverGroups.toArray(new String[this
				.getServerGroups().size()]));
		this.cacheServer.setHostnameForClients(this.getHostNameForClients());
		this.cacheServer.setLoadPollInterval(this.getLoadPollInterval());
		this.cacheServer.setLoadProbe(this.getServerLoadProbe());
		this.cacheServer.setMaxConnections(this.getMaxConnections());
		this.cacheServer.setMaximumMessageCount(this.getMaxMessageCount());
		this.cacheServer.setMaximumTimeBetweenPings(this
				.getMaxTimeBetweenPings());
		this.cacheServer.setMaxThreads(this.getMaxThreads());
		this.cacheServer.setMessageTimeToLive(this.getMessageTimeToLive());
		this.cacheServer.setNotifyBySubscription(this.isNotifyBySubscription());
		this.cacheServer.setPort(this.getPort());
		this.cacheServer.setSocketBufferSize(this.getSocketBufferSize());
		this.cacheServer.getClientSubscriptionConfig().setEvictionPolicy(
				String.valueOf(this.getEvictionPolicy()));
		this.cacheServer.getClientSubscriptionConfig().setCapacity(
				this.getEvictionCapacity());

		if (ConcurrentMap.class.isAssignableFrom(Region.class)) {
			this.cacheServer.getClientSubscriptionConfig().setDiskStoreName(
					this.getDiskStoreName());
		}

		this.cacheServer.getClientSubscriptionConfig().setOverflowDirectory(
				this.getOverFlowDirectory().getAbsolutePath());

		if (this.cacheServer != null) {
			this.configured.compareAndSet(false, true);
		}
	}

	public void init() {
		if (this.cacheServer != null && this.configured.get()) {
			try {
				this.cacheServer.start();
			} catch (IOException e) {
				throw new RuntimeException("Unable to start cache server : "
						+ this.cacheServer, e);
			}
		} else {
			log.warn("Attempt to start cache server before confugring an instance."
					+ getClass().getName()
					+ " .init() was called before the cache server was confiugred.");
		}
	}

	public void destroy() throws Exception {
		if (this.cacheServer != null && this.configured.get()
				&& this.cacheServer.isRunning()) {
			this.cacheServer.stop();
		}
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(final boolean autoStart) {
		this.autoStart = autoStart;
	}

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		if (port <= 0 || port > 65535) {
			throw new RuntimeException(
					"Invalid port. Must be in range (1-65535)");
		}
		this.port = port;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(final int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(final int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public boolean isNotifyBySubscription() {
		return notifyBySubscription;
	}

	public void setNotifyBySubscription(final boolean notifyBySubscription) {
		this.notifyBySubscription = notifyBySubscription;
	}

	public int getSocketBufferSize() {
		return socketBufferSize;
	}

	public void setSocketBufferSize(final int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public int getMaxTimeBetweenPings() {
		return maxTimeBetweenPings;
	}

	public void setMaxTimeBetweenPings(final int maxTimeBetweenPings) {
		this.maxTimeBetweenPings = maxTimeBetweenPings;
	}

	public int getMaxMessageCount() {
		return maxMessageCount;
	}

	public void setMaxMessageCount(final int maxMessageCount) {
		this.maxMessageCount = maxMessageCount;
	}

	public int getMessageTimeToLive() {
		return messageTimeToLive;
	}

	public void setMessageTimeToLive(final int messageTimeToLive) {
		this.messageTimeToLive = messageTimeToLive;
	}

	public List<String> getServerGroups() {
		return Collections.unmodifiableList(serverGroups);
	}

	public void setServerGroups(final List<String> serverGroups) {
		// clear existing groups.
		this.serverGroups.clear();

		// only if any groups are passed in register then else interpret as a
		// clear operation.
		if (serverGroups != null && !serverGroups.isEmpty()) {
			this.serverGroups.addAll(serverGroups);
		}
	}

	public ServerLoadProbe getServerLoadProbe() {
		return serverLoadProbe;
	}

	public void setServerLoadProbe(final ServerLoadProbe serverLoadProbe) {
		if (serverLoadProbe != null) {
			this.serverLoadProbe = serverLoadProbe;
		}
	}

	public long getLoadPollInterval() {
		return loadPollInterval;
	}

	public void setLoadPollInterval(final long loadPollInterval) {
		this.loadPollInterval = loadPollInterval;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public void setBindAddress(final String bindAddress) {
		InetAddress bindAddr = null;
		try {
			bindAddr = InetAddress.getByName(bindAddress);
			this.bindAddress = bindAddr.getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException("Invalid bindAddress supplied : "
					+ bindAddress, e);
		}
	}

	public String getHostNameForClients() {
		return hostNameForClients;
	}

	public void setHostNameForClients(final String hostNameForClients) {
		if (hostNameForClients != null && !hostNameForClients.trim().isEmpty()) {
			this.hostNameForClients = hostNameForClients;
		}
	}

	public EvictionPolicy getEvictionPolicy() {
		return evictionPolicy;
	}

	public void setEvictionPolicy(final EvictionPolicy evictionPolicy) {
		this.evictionPolicy = evictionPolicy;
	}

	public int getEvictionCapacity() {
		return evictionCapacity;
	}

	public void setEvictionCapacity(final int evictionCapacity) {
		this.evictionCapacity = evictionCapacity;
	}

	public String getDiskStoreName() {
		return diskStoreName;
	}

	public void setDiskStoreName(final String diskStoreName) {
		if (diskStoreName != null && !diskStoreName.trim().isEmpty()) {
			this.diskStoreName = diskStoreName;
		}
	}

	public File getOverFlowDirectory() {
		return overFlowDirectory;
	}

	public void setOverFlowDirectory(final File overFlowDirectory) {
		if (overFlowDirectory == null) {
			return;
		}

		this.overFlowDirectory = overFlowDirectory;
	}

	public Set<InterestRegistrationListener> getListeners() {
		return Collections.unmodifiableSet(listeners);
	}

	public void setListeners(
			final Set<InterestRegistrationListener> listeners) {
		// clear existing listeners
		this.listeners.clear();

		// only if any are passed in add to the list else interpret as a clear
		// operation.
		if (listeners != null && !listeners.isEmpty()) {
			this.listeners.addAll(listeners);
		}
	}

	public Cache getCache() {
		return cache;
	}

	public void setCache(final Cache cache) {
		if (cache == null || cache.isClosed()) {
			return;
		}
		this.cache = cache;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CacheServerFactoryBean [beanName=");
		builder.append(beanName);
		builder.append(", autoStart=");
		builder.append(autoStart);
		builder.append(", configured=");
		builder.append(this.configured.get());
		builder.append(", cacheServer=");
		builder.append(cacheServer);
		builder.append("]");
		return builder.toString();
	}

}
