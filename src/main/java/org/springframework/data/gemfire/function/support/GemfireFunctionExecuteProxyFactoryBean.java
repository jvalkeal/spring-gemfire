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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.gemfire.function.DefaultResultCollector;
import org.springframework.data.gemfire.function.FunctionTarget;
import org.springframework.data.gemfire.function.GemfireFunctionExecute;
import org.springframework.data.gemfire.function.GemfireFunctionFilter;
import org.springframework.data.gemfire.function.GemfireFunctionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.cache.execute.ResultCollector;

/**
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionExecuteProxyFactoryBean implements FactoryBean<Object>, InitializingBean {

    private final static Log log = LogFactory.getLog(GemfireFunctionExecuteProxyFactoryBean.class);
    
    /** Target object where function endpoint resides. */
    protected Object target;
    
    /** Created proxy object */
    private Object proxy;
    
    /** Cache */
    private GemFireCache cache;


    @Override
    public Object getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<Object> getObjectType() {
        return Object.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(target.getClass(), getClass().getClassLoader());
        
        Map<Method, MethodAnnotations> mappedAnnotations = new Hashtable<Method, MethodAnnotations>();
        
        // check annotations from class interface methods.
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            GemfireFunctionExecute executeAnnotation = AnnotationUtils.findAnnotation(method, GemfireFunctionExecute.class);
            GemfireFunctionFilter filterAnnotation = null;
            int filterAnnotationParameter = -1;
            if(executeAnnotation != null) {
                Method mappedMethod = null;
                for(Class<?> clazz: ifcs) {
                    try {
                        mappedMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                        
                        // find GemfireFunctionFilter annotations and index of parameter
                        Annotation[][] parameters = method.getParameterAnnotations();
                        for (int i = 0; i < parameters.length; i++) {
                            Annotation[] annotations = parameters[i];
                            for (Annotation annotation : annotations) {
                                if(annotation instanceof GemfireFunctionFilter) {
                                    filterAnnotation = (GemfireFunctionFilter) annotation;
                                    filterAnnotationParameter = i;
                                    break;
                                }
                            }
                        }
                        
                        break;
                    } catch (Exception e) {
                    }
                }
                if(mappedMethod != null) {
                    mappedAnnotations.put(mappedMethod, new MethodAnnotations(executeAnnotation, filterAnnotation, filterAnnotationParameter));                    
                }
            }
        }
        
        AnnotatedInterceptingInvocationHandler aiih = new AnnotatedInterceptingInvocationHandler(target, mappedAnnotations);
        
        proxy = Proxy.newProxyInstance(getClass().getClassLoader(), ifcs, aiih);
    }
    
    /**
     * @param target the target to set
     */
    public void setTarget(Object target) {
        this.target = target;
    }
    
    
    /**
     * @param cache the cache to set
     */
    public void setCache(GemFireCache cache) {
        this.cache = cache;
    }

    private class AnnotatedInterceptingInvocationHandler implements InvocationHandler {
        
        Object invokeTarget;
        Map<Method, MethodAnnotations> mappedAnnotations;

        public AnnotatedInterceptingInvocationHandler(Object invokeTarget, Map<Method, MethodAnnotations> mappedAnnotations) {
            super();
            this.invokeTarget = invokeTarget;
            this.mappedAnnotations = mappedAnnotations;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            GemfireFunctionExecute executeAnnotation = this.mappedAnnotations.get(method).executeAnnotation;
            GemfireFunctionFilter filterAnnotation = this.mappedAnnotations.get(method).filterAnnotation;
            int filterArgIndex = this.mappedAnnotations.get(method).filterAnnotationParameter;
            // if we have annotated method, intercept for function execution
            if(executeAnnotation != null) {
                GemfireFunctionTemplate template = new GemfireFunctionTemplate(cache);
                
                String functionName = StringUtils.hasText(executeAnnotation.id()) ? executeAnnotation.id() : method.getName();
                ResultCollector<? extends Serializable,? extends Serializable> collector = null;
                if(!executeAnnotation.collector().equals(DefaultResultCollector.class)) {
                    collector = (ResultCollector<? extends Serializable, ? extends Serializable>) BeanUtils.instantiate(executeAnnotation.collector());
                }
                
                if(executeAnnotation.target() == FunctionTarget.ON_ALL_DS_MEMBERS) {
                    return template.executeOnMembers(functionName, null, collector, executeAnnotation.value(), executeAnnotation.timeout());                    
                } else if(executeAnnotation.target() == FunctionTarget.ON_REGION) {
                    Set<?> filter = null;
                    if(filterArgIndex > -1) {
                        filter = (Set<?>)args[filterArgIndex];
                    }
                    return template.executeOnRegion(functionName, null, collector, filter, executeAnnotation.value(), executeAnnotation.timeout());
                } else {
                    // we should not get here, unhandled FunctionTarget
                    return null;
                }
            }            

            // pass through other methods
            try {
                return method.invoke(this.invokeTarget, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
            
        }
        
    }
    
    /** Helper class to store method annotations. */
    private class MethodAnnotations {
        GemfireFunctionExecute executeAnnotation;
        GemfireFunctionFilter filterAnnotation;
        int filterAnnotationParameter = -1;
        public MethodAnnotations(GemfireFunctionExecute executeAnnotation, GemfireFunctionFilter filterAnnotation, int filterAnnotationParameter) {
            super();
            this.executeAnnotation = executeAnnotation;
            this.filterAnnotation = filterAnnotation;
            this.filterAnnotationParameter = filterAnnotationParameter;
        }        
    }
    
}
