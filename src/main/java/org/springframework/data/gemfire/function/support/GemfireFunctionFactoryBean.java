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
package org.springframework.data.gemfire.function.support;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionService;

/**
 * Simple factory bean which helps instantiation and registration
 * of Gemfire Function.
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionFactoryBean implements FactoryBean<Function>, InitializingBean {

    /** Function target class */
    private Class<Function> targetClass;

    /** Function target class */
    private String targetClassName;

    /** Function created by this factory */
    private Function function;

    public GemfireFunctionFactoryBean() {
        super();
        this.targetClass = null;
        this.targetClassName = null;
    }

    @Override
    public Function getObject() throws Exception {
        return function;
    }

    @Override
    public Class<Function> getObjectType() {
        return Function.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(targetClass==null && targetClassName==null, "Either function class or its classname must be set");
        if(targetClass==null) {
            targetClass = (Class<Function>) ClassUtils.forName(targetClassName, getClass().getClassLoader());
        }
        function = BeanUtils.instantiate(targetClass);
        FunctionService.registerFunction(function);
    }    
    
    /**
     * Sets target class which this factory is using
     * to instantiate a Gemfire Function instance.
     * 
     * @param target the target to set
     */
    public void setTargetClass(Class<Function> targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * Sets target class name which this factory is using
     * to instantiate a Gemfire Function instance.
     * 
     * @param target the target to set
     */
    public void setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName;
    }

}
