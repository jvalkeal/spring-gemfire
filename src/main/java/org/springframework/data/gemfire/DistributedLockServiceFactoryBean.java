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
package org.springframework.data.gemfire;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.gemfire.dlock.GemfireDistributedLockService;

/**
 * Factory bean for bean handling native gemfire distributed locking services.
 * 
 * @author Janne Valkealahti
 *
 */
public class DistributedLockServiceFactoryBean implements FactoryBean<GemfireDistributedLockService>, InitializingBean {

    private Properties defaultProperties;
    private Map<String, Properties> preCreate = Collections.emptyMap();

    public void setDefaultProperties(Properties properties) {
        this.defaultProperties = properties;
    }
    
    public void setPreCreateServices(Map<String, Properties> preCreate) {
        this.preCreate = preCreate;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public GemfireDistributedLockService getObject() throws Exception {
        
        long wait = getPropertyValue(defaultProperties, GemfireDistributedLockService.KEY_WAITTIMEMILLIS, GemfireDistributedLockService.DEFAULT_WAITTIMEMILLIS);
        long lease = getPropertyValue(defaultProperties, GemfireDistributedLockService.KEY_LEASETIMEMILLIS, GemfireDistributedLockService.DEFAULT_LEASETIMEMILLIS);
                
        GemfireDistributedLockService gemfireDistributedLockService = new GemfireDistributedLockService(wait, lease, preCreate);
        return gemfireDistributedLockService;
    }

    @Override
    public Class<GemfireDistributedLockService> getObjectType() {
        return GemfireDistributedLockService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
    
    private long getPropertyValue(Properties properties, String key, long defValue) {
        long val = defValue;
        try {
            val = Long.parseLong(properties.getProperty(key));
        } catch (Exception e) {}
        return val;
    }

}
