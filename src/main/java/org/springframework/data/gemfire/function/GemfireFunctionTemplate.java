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

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.gemfire.GemfireCacheUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionService;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.client.PoolManager;
import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedSystem;

/**
 * @author David Turanski
 * @author Janne Valkealahti
 */
public class GemfireFunctionTemplate implements InitializingBean, GemfireFunctionOperations {

    public static final long NO_TIMEOUT = -1;
    
    /** Logger available to subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Default region can be set to have easier access to it
     * without having a need to explicitly define it in repository
     * annotations. Most of the time a repository is associated to one
     * region, template and domain class.
     */
    private Region<?, ?> defaultRegion;

    /**
     * Region service is needed for some of the function
     * executions.
     */
    private RegionService cache;
    
    public GemfireFunctionTemplate() {

    }

    /**
     * 
     * @param cache
     */
    public GemfireFunctionTemplate(RegionService cache) {
        this.cache = cache;
        afterPropertiesSet();
    }

    public GemfireFunctionTemplate(Region region) {
        this.defaultRegion = region;
        afterPropertiesSet();
    }

    /**
     * Sets the default regions.
     * @param region The regions
     */
    public void setDefaultRegion(Region<?, ?> region) {
        defaultRegion = region;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        if (defaultRegion != null) {
            this.cache = defaultRegion.getRegionService();
        }
    }


    /**
     * 
     * @param regionId
     * @return
     */
    public Region<?, ?> getRegion(String regionId) {
        return this.cache.getRegion(regionId);
    }

    /**
     * 
     */
    @Override
    public <T> T executeOnServerCache(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_SERVER_CACHE, parameters, collector, null, value, timeout);
    }

    /**
     * 
     */
    @Override
    public <T> T executeOnServersCache(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_SERVER_CACHE, parameters, collector, null, value, timeout);
    }
    
    /**
     * 
     */
    @Override
    public <T> T executeOnServerPool(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_SERVER_POOL, parameters, collector, null, value, timeout);
    }

    /**
     * 
     */
    @Override
    public <T> T executeOnServersPool(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_SERVER_POOL, parameters, collector, null, value, timeout);
    }

    /**
     * 
     */
    @Override
    public <T> T executeOnMembers(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_ALL_DS_MEMBERS, parameters, collector, null, value, timeout);
    }

    /**
     * 
     */
    @Override
    public <T> T executeOnRegion(final String functionId, final Object[] parameters,
            final ResultCollector<? extends Serializable,? extends Serializable> collector, Set<?> filter, final String value, final long timeout) {
        return execute(new GemfireFunctionCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T doInGemfire(Execution execution) {
                return (T) execution.execute(functionId);
            }
        }, FunctionTarget.ON_REGION, parameters, collector, filter, value, timeout);
    }
    
    /**
     * Execute given callback in a given target.
     * 
     * @param callback Callback object that specifies the Gemfire function action
     * @param target Define the function target
     * @param parameters Parameters passed to function execution
     * @param collector Custom result collector
     * @param value String value defining region or pool depending on used target.
     * @param timeout Function execution timeout
     * @return Object returned from the function execution
     */
    public <T> T execute(GemfireFunctionCallback<T> callback, FunctionTarget target, Object[] parameters,
            ResultCollector<? extends Serializable,? extends Serializable> collector, Set<?> filter, String value, long timeout) {
        Assert.notNull(callback, "Callback object must not be null");
        
        try {
            
            // Find the function execution
            Execution execution = null;
            
            // use either default or named region
            if (target == FunctionTarget.ON_REGION) {
                Region<?, ?> region = StringUtils.hasText(value) ? getRegion(value) : defaultRegion;
                execution = FunctionService.onRegion(region);
            } else if (target == FunctionTarget.ON_ALL_DS_MEMBERS) {
                execution = FunctionService.onMembers(findRunningDs());
            } else if (target == FunctionTarget.ON_SERVER_POOL) {
                Pool pool = PoolManager.find(value);
                execution = FunctionService.onServer(pool);
            } else if (target == FunctionTarget.ON_SERVERS_POOL) {
                Pool pool = PoolManager.find(value);
                execution = FunctionService.onServers(pool);
            } else if (target == FunctionTarget.ON_SERVER_CACHE) {
                execution = FunctionService.onServer(cache);
            } else if (target == FunctionTarget.ON_SERVERS_CACHE) {
                execution = FunctionService.onServer(cache);
            } else {
                // we should not get here
                Assert.notNull(execution, "We must have function execution");
                throw new InvalidDataAccessApiUsageException(
                        "Function target " + target + " not regognised, unable to get function execution.");
            }
            
            execution = addExecutionOptions(execution, parameters, collector, filter);

            // let the callback handle the execution and
            // return results
            ResultCollector resultCollector = (ResultCollector) callback.doInGemfire(execution);
            if(timeout < 1) {
                return (T) resultCollector.getResult();
            } else {
                return (T) resultCollector.getResult(timeout, TimeUnit.MILLISECONDS);                
            }
            
        } catch (InterruptedException e) {
            throw new DataAccessResourceFailureException("Function execution interrupted.", e);
        } catch (FunctionException e) {
            throw GemfireCacheUtils.convertGemfireAccessException(e);
        }

    }
    
    private static Execution addExecutionOptions(Execution execution, Serializable args, ResultCollector<? extends Serializable,? extends Serializable> rc, Set<?> filter) {
        Execution ret = execution;
        if(args != null) {
            ret = ret.withArgs(args);
        }
        if(rc != null) {
            ret = ret.withCollector(rc);
        }
        if(filter != null && !filter.isEmpty()) {
            ret = ret.withFilter(filter);
        }
        return ret;
    }
    
    private static DistributedSystem findRunningDs() {
        
        // we'll find running ds if we're on running cache
        // node and cache exists.
        try {
            Cache anyInstance = CacheFactory.getAnyInstance();
            DistributedSystem distributedSystem = anyInstance.getDistributedSystem();
            return distributedSystem;
        } catch (Exception e) {
        }
        
        return null;
    }
        
}
