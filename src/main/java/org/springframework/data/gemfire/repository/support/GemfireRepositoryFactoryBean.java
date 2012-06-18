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
package org.springframework.data.gemfire.repository.support;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.gemfire.dlock.GemfireDistributedLockService;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.gemfire.mapping.GemfirePersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import com.gemstone.bp.edu.emory.mathcs.backport.java.util.Collections;
import com.gemstone.gemfire.cache.Region;

/**
 * {@link FactoryBean} adapter for {@link GemfireRepositoryFactory}.
 * 
 * @author Oliver Gierke
 */
public class GemfireRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
RepositoryFactoryBeanSupport<T, S, ID> implements ApplicationContextAware {

	private MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> context;
	private Iterable<Region<?, ?>> regions;
	private GemfireDistributedLockService gemfireDistributedLockService;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Collection<Region> regions = applicationContext.getBeansOfType(Region.class).values();
		this.regions = Collections.unmodifiableCollection(regions);
		gemfireDistributedLockService = getGemfireDistributedLockService(applicationContext);
	}

	/**
	 * Configures the {@link MappingContext} to be used.
	 * 
	 * @param context the context to set
	 */
	public void setMappingContext(MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> context) {
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#createRepositoryFactory()
	 */
	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return new GemfireRepositoryFactory(regions, context, gemfireDistributedLockService);
	}
	
	/**
	 * Finds distributed locking service from application context.
	 * 
	 * @param applicationContext
	 * @return Distributed locking service if exists, null otherwise.
	 */
    protected GemfireDistributedLockService getGemfireDistributedLockService(ApplicationContext applicationContext) {
        GemfireDistributedLockService gemfireDistributedLockService = null;
        Map<String, GemfireDistributedLockService> dLockServices = null;
        try {
            dLockServices = applicationContext.getBeansOfType(GemfireDistributedLockService.class);
        } catch (BeansException e) {
            // no dlocks
        }
        // get bean with name "distributedLockService" or one from a map
        if(dLockServices != null) {
            gemfireDistributedLockService = dLockServices.get("distributedLockService");
            if(gemfireDistributedLockService == null) {
                Set<Entry<String, GemfireDistributedLockService>> entrySet = dLockServices.entrySet();
                Iterator<Entry<String, GemfireDistributedLockService>> iterator = entrySet.iterator();
                if(iterator.hasNext()) {
                    gemfireDistributedLockService = iterator.next().getValue();
                }
            }            
        }
        return gemfireDistributedLockService;
    }

}
