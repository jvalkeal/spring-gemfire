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
import java.lang.reflect.Method;

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.dlock.DistibutedLockRepositoryPostProcessor;
import org.springframework.data.gemfire.dlock.GemfireDistributedLockService;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.gemfire.mapping.GemfirePersistentProperty;
import org.springframework.data.gemfire.mapping.Regions;
import org.springframework.data.gemfire.repository.query.DefaultGemfireEntityInformation;
import org.springframework.data.gemfire.repository.query.GemfireEntityInformation;
import org.springframework.data.gemfire.repository.query.GemfireQueryMethod;
import org.springframework.data.gemfire.repository.query.PartTreeGemfireRepositoryQuery;
import org.springframework.data.gemfire.repository.query.StringBasedGemfireRepositoryQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Region;

/**
 * {@link RepositoryFactorySupport} implementation creating repository proxies for Gemfire.
 * 
 * @author Oliver Gierke
 */
public class GemfireRepositoryFactory extends RepositoryFactorySupport {

	private final MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> context;
	private final Regions regions;
	private GemfireDistributedLockService gemfireDistributedLockService;

	/**
	 * Creates a new {@link GemfireRepositoryFactory}.
	 * 
	 * @param regions must not be {@literal null}.
	 * @param context
     * @param gemfireDistributedLockService Distributed locking service.
	 */
	public GemfireRepositoryFactory(Iterable<Region<?, ?>> regions,
			MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> context,
			GemfireDistributedLockService gemfireDistributedLockService) {

		Assert.notNull(regions);

		this.context = context == null ? new GemfireMappingContext() : context;
		this.regions = new Regions(regions, this.context);
		this.gemfireDistributedLockService = gemfireDistributedLockService;
	}
	
    /**
     * Creates a new {@link GemfireRepositoryFactory}.
     * 
     * @param regions must not be {@literal null}.
     * @param context
     */
	public GemfireRepositoryFactory(Iterable<Region<?, ?>> regions,
	        MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> context) {
	    this(regions, context, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> GemfireEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		GemfirePersistentEntity<T> entity = (GemfirePersistentEntity<T>) context.getPersistentEntity(domainClass);
		return new DefaultGemfireEntityInformation<T, ID>(entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object getTargetRepository(RepositoryMetadata metadata) {

		GemfireEntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());
		GemfireTemplate gemfireTemplate = getTemplate(metadata);
		
		if(gemfireDistributedLockService != null) {
		    // pass repository interface name which can be used
		    // as a default dlock service name if needed.
	        String repoInterfaceName = metadata.getRepositoryInterface().getName();
	        addRepositoryProxyPostProcessor(new DistibutedLockRepositoryPostProcessor(gemfireDistributedLockService, repoInterfaceName));		    
		}

		return new SimpleGemfireRepository(gemfireTemplate, entityInformation);
	}

	private GemfireTemplate getTemplate(RepositoryMetadata metadata) {

		Class<?> domainClass = metadata.getDomainType();
		GemfirePersistentEntity<?> entity = context.getPersistentEntity(domainClass);

		Region<?, ?> region = regions.getRegion(domainClass);

		if (region == null) {
			throw new IllegalStateException(String.format("No region '%s' found for domain class %s! Make sure you have "
					+ "configured a Gemfire region of that name in your application context!", entity.getRegionName(), domainClass));
		}

		Class<?> regionKeyType = region.getAttributes().getKeyConstraint();
		Class<?> entityIdType = metadata.getIdType();

		if (regionKeyType != null && entity.getIdProperty() != null) {
			Assert.isTrue(regionKeyType.isAssignableFrom(entityIdType), String.format(
					"The region referenced only supports keys of type %s but the entity to be stored has an id of type %s!",
					regionKeyType, entityIdType));
		}

		return new GemfireTemplate(region);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleGemfireRepository.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key)
	 */
	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
		return new QueryLookupStrategy() {
			@Override
			public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

				GemfireQueryMethod queryMethod = new GemfireQueryMethod(method, metadata, context);
				GemfireTemplate template = getTemplate(metadata);

				if (queryMethod.hasAnnotatedQuery()) {
					return new StringBasedGemfireRepositoryQuery(queryMethod, template);
				}

				String namedQueryName = queryMethod.getNamedQueryName();
				if (namedQueries.hasQuery(namedQueryName)) {
					return new StringBasedGemfireRepositoryQuery(namedQueries.getQuery(namedQueryName), queryMethod, template);
				}

				return new PartTreeGemfireRepositoryQuery(queryMethod, template);
			}
		};
	}
}
