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
package org.springframework.data.gemfire.dlock;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.distributed.DistributedLockService;
import com.gemstone.gemfire.distributed.DistributedSystem;

/**
 * Bean wrapping gemfire distributed locking service.
 * <p>
 * <b>waitTimeMillis</b> - the number of milliseconds to try to acquire the lock
 * before giving up and returning false. A value of -1 causes this method to block
 * until the lock is acquired. A value of 0 causes this method to return false without
 * waiting for the lock if the lock is held by another member or thread.
 * <p>
 * <b>leaseTimeMillis</b> - the number of milliseconds to hold the lock after granting it,
 * before automatically releasing it if it hasn't already been released by invoking
 * unlock(Object). If leaseTimeMillis is -1, hold the lock until explicitly unlocked.
 * <p>
 * <b>Pre create lock services:</b>
 * <p>
 * 
 * @author Janne Valkealahti
 *
 */
public class GemfireDistributedLockService {
    
    public final static Log log = LogFactory.getLog(GemfireDistributedLockService.class);
    
    // default values for wait and lease
    public final static long DEFAULT_WAITTIMEMILLIS = 30000;
    public final static long DEFAULT_LEASETIMEMILLIS = 60000;
    
    // keys when storing wait and lease times
    public final static String KEY_WAITTIMEMILLIS = "waitTimeMillis";
    public final static String KEY_LEASETIMEMILLIS = "leaseTimeMillis";
    
    // global wait and lease times if not explicitely set per
    // service or if not given with method
    private long waitTimeMillis;
    private long leaseTimeMillis;
    
    // storing wait and lease times for services. 
    private Map<String, Map<String, Long>> serviceValues = new Hashtable<String, Map<String,Long>>(2);
    
    /**
     * Create a lock service with default wait and lease times and
     * without pre-defined services.
     */
    public GemfireDistributedLockService() {
        this(DEFAULT_WAITTIMEMILLIS, DEFAULT_LEASETIMEMILLIS, Collections.<String, Properties> emptyMap());
    }

    /**
     * Create a lock service with given global wait and lease times and optional map defining
     * pre created services.
     * @param waitTimeMillis Default wait time to use if not explicitly set in other methods.
     * @param leaseTimeMillis Default lease time to use if not explicitly set in other methods.
     * @param preCreate Map containing pre-defined services and optional wait and lease
     *                  times per service.
     */
    public GemfireDistributedLockService(long waitTimeMillis, long leaseTimeMillis, Map<String, Properties> preCreate) {
        super();
        this.waitTimeMillis = waitTimeMillis;
        this.leaseTimeMillis = leaseTimeMillis;
        doPrecreate(preCreate);
    }

    /**
     * Acquire lock for object from a given service name.
     *  
     * @param serviceName Distributed lock service name
     * @param object Object lock
     */
    public void lock(String serviceName, Object object) {
        DistributedLockService dls = getDistributedLockService(serviceName);
        dls.lock(
                object, 
                serviceValues
                .get(serviceName)
                .get(KEY_WAITTIMEMILLIS),
                serviceValues
                .get(serviceName)
                .get(KEY_WAITTIMEMILLIS));
    }

    /**
     * Acquire lock for object from a given service name.
     * 
     * @param serviceName Distributed lock service name
     * @param object Object lock
     * @param waitTimeMillis 
     * @param leaseTimeMillis
     */
    public void lock(String serviceName, Object object, long waitTimeMillis, long leaseTimeMillis) {
        DistributedLockService dls = getDistributedLockService(serviceName);
        dls.lock(object, getMillisValue(serviceName, KEY_WAITTIMEMILLIS, waitTimeMillis), getMillisValue(serviceName, KEY_LEASETIMEMILLIS, leaseTimeMillis));
    }
    
    /**
     * Unlock the distributed lock.
     * 
     * @param serviceName Distributed lock service name
     * @param object Object lock
     */
    public void unlock(String serviceName, Object object) {
        DistributedLockService dls = getDistributedLockService(serviceName);
        dls.unlock(object);
    }
    
    /**
     * 
     * @param serviceName Distributed lock service name
     * @return DistributedLockService or null if something went wrong.
     */
    public DistributedLockService getDistributedLockService(String serviceName) {
        DistributedLockService dls = DistributedLockService.getServiceNamed(serviceName);
        if(dls == null) {
            try {
                // need to lock creation!!!
                DistributedSystem ds = CacheFactory.getAnyInstance().getDistributedSystem();
                Map<String, Long> map = new Hashtable<String, Long>(2);
                map.put(KEY_WAITTIMEMILLIS, waitTimeMillis);
                map.put(KEY_LEASETIMEMILLIS, leaseTimeMillis);
                serviceValues.put(serviceName, map);
                dls = DistributedLockService.create(serviceName, ds);
            } catch (Exception e) {
                log.error(e);
                // try again
                dls = DistributedLockService.getServiceNamed(serviceName);
//                return null;
            }
        }
        return dls;
    }
    
    // helper function to handle wait/lease values
    private long getMillisValue(String serviceName, String key, long explicitelySet) {
        if (explicitelySet > -2) {
            return explicitelySet;
        }
        long value = serviceValues.get(serviceName).get(key);
        if(value < -1) {
            if(key.equals(KEY_WAITTIMEMILLIS)) {
                value = waitTimeMillis;
            } else if (key.equals(KEY_LEASETIMEMILLIS)) {
                value = leaseTimeMillis;                
            }
        }
        return value;
    }
    
    // helper function to precreate defined lock services
    private void doPrecreate(Map<String, Properties> preCreate) {
        if(preCreate == null) {
            return;
        }
        Iterator<Entry<String, Properties>> iterator = preCreate.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Properties> entry = (Map.Entry<String, Properties>) iterator.next();
            
            String waitProperty = entry.getValue().getProperty(KEY_WAITTIMEMILLIS);
            long wait = waitTimeMillis;
            try {
                wait = Long.parseLong(waitProperty);
            } catch (NumberFormatException e) {}

            String leaseProperty = entry.getValue().getProperty(KEY_LEASETIMEMILLIS);
            long lease = leaseTimeMillis;
            try {
                lease = Long.parseLong(leaseProperty);
            } catch (NumberFormatException e) {}

            getDistributedLockService(entry.getKey());
            Map<String, Long> map = new Hashtable<String, Long>(2);
            map.put(KEY_WAITTIMEMILLIS, wait);
            map.put(KEY_LEASETIMEMILLIS, lease);
            serviceValues.put(entry.getKey(), map);
        }
    }
    
}
