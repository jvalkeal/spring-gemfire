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
package org.springframework.data.gemfire.function.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.gemfire.function.GemfireFunction;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.execute.Function;

/**
 * Factory bean for GemfireFunctionService.
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionServiceFactoryBean implements InitializingBean, FactoryBean<GemfireFunctionService> {
    
    private final static Log log = LogFactory.getLog(GemfireFunctionServiceFactoryBean.class);

    /** List of function class names. */
    private List<String> functions;

    /** Base package to scan. */
    private String basePackage;
    
    /** Flag to scan Gemfire Function interfaces. */
    private boolean scanInterface;
    
    /** Flag to scan annotated functions. */
    private boolean scanAnnotated;
    
    /** Flag returned service. */
    private GemfireFunctionService gemfireFunctionService;
    
    /**
     * Default constructor.
     */
    public GemfireFunctionServiceFactoryBean() {
        super();
        this.functions = null;
        this.scanAnnotated = false;
        this.scanInterface = false;
        this.basePackage = null;
    }

    @Override
    public GemfireFunctionService getObject() throws Exception {
        return gemfireFunctionService;
    }

    @Override
    public Class<GemfireFunctionService> getObjectType() {
        return GemfireFunctionService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(scanAnnotated || scanInterface) {
            Assert.notNull(basePackage, "Function scanning is activated, base-package must be set.");            
        }
        if(gemfireFunctionService == null) {
            gemfireFunctionService = initFunctionService();
            gemfireFunctionService.registerFunctions();
        }        
    }

    /**
     * @param functions the functions to set
     */
    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }

    /**
     * @param basePackage the basePackage to set
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * @param scanInterfaces the scanInterfaces to set
     */
    public void setScanInterface(boolean scanInterface) {
        this.scanInterface = scanInterface;
    }

    /**
     * @param scanAnnotated the scanAnnotated to set
     */
    public void setScanAnnotated(boolean scanAnnotated) {
        this.scanAnnotated = scanAnnotated;
    }
    
    /**
     * Builds a GemfireFunctionService bean.
     * 
     * @return the gemfire function service bean
     */
    protected GemfireFunctionService initFunctionService() {
        GemfireFunctionService functionService = new GemfireFunctionService();
        
        List<String> uniqueClazzNames = new ArrayList<String>();
        
        // explicitely defined function classes
        if(functions != null) {
            uniqueClazzNames.addAll(functions);            
        }
        
        // scanned classes implementing Function interface
        if(scanInterface) {
            for (String clazzName : scanExistingInterfaces()) {
                if(!uniqueClazzNames.contains(clazzName)) {
                    uniqueClazzNames.add(clazzName);
                }
            }            
        }

        // create proxy functions from annotations
        List<Function> annotatedFunctions = new ArrayList<Function>();
        if(scanAnnotated) {
            for(AnnotatedFunction annotatedFunction : scanExistingAnnotations()) {
                Object target = BeanUtils.instantiate(annotatedFunction.clazz);
                GemfireFunctionProxyFactoryBean factory = new GemfireFunctionProxyFactoryBean();
                factory.setTarget(target);                
                factory.setMethod(annotatedFunction.method);
                String functionName = StringUtils.hasText(annotatedFunction.annotation.id()) ? annotatedFunction.annotation.id() : annotatedFunction.method.getName();
                factory.setFunctionName(functionName);
                try {
                    factory.afterPropertiesSet();
                    Function function = factory.getObject();
                    annotatedFunctions.add(function);
                } catch (Exception e) {
                    log.warn("Can't create a proxied function.", e);
                }
                
            }
        }        

        // instantiate native functions
        List<Function> nativeFunctions = new ArrayList<Function>();
        for (String clazzName : uniqueClazzNames) {
            try {
                Class<?> clazz = ClassUtils.forName(clazzName, this.getClass().getClassLoader());
                // check that class has a Function interface and visible constructor
                if(ClassUtils.isAssignable(Function.class, clazz) && ClassUtils.hasConstructor(clazz)) {
                    nativeFunctions.add((Function)BeanUtils.instantiate(clazz));
                }
            } catch (Exception e) {
            }            
        }
        
        // join lists and add to service
        List<Function> joined = new ArrayList<Function>(nativeFunctions);
        joined.addAll(annotatedFunctions);
        functionService.setFunctions(joined);
        
        return functionService;
    }
    
    /**
     * Scans all classes having Function interface.
     * 
     * @return List of full class names having Function interface.
     */
    protected List<String> scanExistingInterfaces() {
        List<String> clazzList = new ArrayList<String>();
        
        ClassPathScanningCandidateComponentProvider foo = new ClassPathScanningCandidateComponentProvider(true);
        foo.addIncludeFilter(new AssignableTypeFilter(Function.class));
        Set<BeanDefinition> findCandidateComponents = foo.findCandidateComponents(basePackage);
        for (BeanDefinition beanDefinition : findCandidateComponents) {
            clazzList.add(beanDefinition.getBeanClassName());
        }
        
        return clazzList;
    }

    /**
     * Scans all classes having GemfireFunction annotation.
     * 
     * @return List of full class names having GemfireFunction annotation.
     */
    protected List<AnnotatedFunction> scanExistingAnnotations() {
        List<AnnotatedFunction> functionList = new ArrayList<AnnotatedFunction>();
        
        ClassPathScanningCandidateComponentProvider foo = new ClassPathScanningCandidateComponentProvider(true);
        foo.addIncludeFilter(new AnnotationTypeFilter(org.springframework.data.gemfire.function.GemfireFunctionService.class));
        Set<BeanDefinition> findCandidateComponents = foo.findCandidateComponents(basePackage);
        for (BeanDefinition beanDefinition : findCandidateComponents) {
            try {
                Class<?> clazz = ClassUtils.forName(beanDefinition.getBeanClassName(), getClass().getClassLoader());
                for(Method method : clazz.getMethods()) {
                    GemfireFunction annotation = AnnotationUtils.getAnnotation(method, GemfireFunction.class);
                    if(annotation != null) {
                        functionList.add(new AnnotatedFunction(clazz, method, annotation));
                    }
                }
            } catch (ClassNotFoundException e) {
                // should not happen
            }
        }
       
        return functionList;
    }
    
    /**
     * Helper class to store needed info for annotated functions.
     */
    private class AnnotatedFunction {
        Class<?> clazz;
        Method method;
        GemfireFunction annotation;
        public AnnotatedFunction(Class<?> clazz, Method method, GemfireFunction annotation) {
            super();
            this.clazz = clazz;
            this.method = method;
            this.annotation = annotation;
        }        
    }

}
