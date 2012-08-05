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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.data.gemfire.function.GemfireFunction;
import org.springframework.data.gemfire.function.GemfireFunctionExecute;
import org.springframework.data.gemfire.function.GemfireFunctionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * A bean definition scanner that detects bean candidates on the classpath,
 * registering corresponding bean definitions with a given registry ({@code BeanFactory}
 * or {@code ApplicationContext}).
 *
 * <p>Candidate classes are detected through configurable type filters. The
 * default filters include classes that are annotated with 
 * {@link org.springframework.data.gemfire.function.GemfireFunctionService @GemfireFunctionService}
 * or implement {@link com.gemstone.gemfire.cache.execute.Function Function} interface.
 * 
 * <p>Additionally classes with {@link org.springframework.data.gemfire.function.GemfireFunctionService @GemfireFunctionService}
 * annotation is scanned to find methods annotated with 
 * {@link org.springframework.data.gemfire.function.GemfireFunction @GemfireFunction} and
 * {@link org.springframework.data.gemfire.function.GemfireFunctionExecute @GemfireFunctionExecute}. These
 * methods will be created as a proxy functions and proxy function executions.
 * 
 * <p>Bean naming policy for created beans:
 * 
 * <p>
 * <ul>
 * <li>Native function: <b>gemfireFunctionFactoryBean-ValueSumFunction</b>
 * <li>Base bean containing custom implementations: <b>customFunctionsImpl</b>
 * <li>Proxied function: <b>gemfireFunctionProxyFactoryBean-functionIdContextResultsVoid</b>
 * <li>Proxied function execution: <b>gemfireFunctionExecuteProxyFactoryBean-customFunctionsImpl</b>
 * </ul>
 * 
 * @author Janne Valkealahti
 */
public class FunctionBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {
    
    public final static String GEMFIREFUNCTIONFACTORYBEAN_CLASSNAME = GemfireFunctionFactoryBean.class.getName();
    public final static String GEMFIREFUNCTIONPROXYFACTORYBEAN_CLASSNAME = GemfireFunctionProxyFactoryBean.class.getName();
    public final static String GEMFIREFUNCTIONEXECUTEPROXYFACTORYBEAN_CLASSNAME = GemfireFunctionExecuteProxyFactoryBean.class.getName();

    public final static String GEMFIREFUNCTIONFACTORYBEAN_SIMPLE_CLASSNAME = GemfireFunctionFactoryBean.class.getSimpleName();
    public final static String GEMFIREFUNCTIONPROXYFACTORYBEAN_SIMPLE_CLASSNAME = GemfireFunctionProxyFactoryBean.class.getSimpleName();
    public final static String GEMFIREFUNCTIONEXECUTEPROXYFACTORYBEAN_SIMPLE_CLASSNAME = GemfireFunctionExecuteProxyFactoryBean.class.getSimpleName();
    
    public final static String BEAN_PROPERTY_TARGET = "target";
    public final static String BEAN_PROPERTY_METHODNAME = "methodName";
    public final static String BEAN_PROPERTY_TARGETCLASSNAME = "targetClassName";
    public final static String BEAN_PROPERTY_FUNCTIONNAME = "functionName";
    public final static String BEAN_PROPERTY_CACHE = "cache";
        
    private final BeanDefinitionRegistry registry;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

    private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();
    
    private String[] autowireCandidatePatterns;

    public FunctionBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this(registry, getOrCreateEnvironment(registry));
    }
    
    public FunctionBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment) {
        super(false, environment);

        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        this.registry = registry;

        // Determine ResourceLoader to use.
        if (this.registry instanceof ResourceLoader) {
            setResourceLoader((ResourceLoader) this.registry);
        }
    }

    /**
     * Scan given packages to find candidate classes which will be a source
     * for function beans, function proxy beans and function execute beans.
     * 
     * @param functionClasses explicit list of function classes to create
     * @param basePackages
     * @return
     */
    public Set<BeanDefinitionHolder> doScan(List<String> functionClasses, String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
        
        
        for (String basePackage : basePackages) {
            
            Set<BeanDefinition> componentCandidates = findCandidateComponents(basePackage);
            for (BeanDefinition componentCandidate : componentCandidates) {
                                
                List<BeanDefinition> candidates = createFunctionBeanDefinitions(componentCandidate);
                for (BeanDefinition candidate : candidates) {

                    ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
                    candidate.setScope(scopeMetadata.getScopeName());
                    
                    String beanName = generateBeanName(candidate);
                    if (candidate instanceof AbstractBeanDefinition) {
                        postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
                    }
                                        
                    if (checkCandidate(beanName, candidate)) {
                        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                        beanDefinitions.add(definitionHolder);
                        registerBeanDefinition(definitionHolder, this.registry);
                    }
                    
                }               
            }                       
        }

        // if caller didn't give any native function classes, bail out..
        if(functionClasses.isEmpty()) {
            return beanDefinitions;
        }
        
        // adding explicit native function, check that bean
        // wasn't created during the classpath scan.
        Set<String> existing = new HashSet<String>();
        for (BeanDefinitionHolder holder : beanDefinitions) {
            String beanClassName = holder.getBeanDefinition().getBeanClassName();
            if (beanClassName.equals(FunctionBeanDefinitionScanner.GEMFIREFUNCTIONFACTORYBEAN_CLASSNAME)) {
                PropertyValue property = holder.getBeanDefinition().getPropertyValues().getPropertyValue("targetClassName");
                existing.add((String) property.getValue());
            }
        }
        
        for(String functionClazz : functionClasses) {
            if (!existing.contains(functionClazz)) {
                GenericBeanDefinition candidate = new GenericBeanDefinition();
                candidate.setBeanClassName(GEMFIREFUNCTIONFACTORYBEAN_CLASSNAME);
                candidate.getPropertyValues().add(BEAN_PROPERTY_TARGETCLASSNAME, functionClazz);
                
                ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
                candidate.setScope(scopeMetadata.getScopeName());
                
                String beanName = generateBeanName(candidate);
                if (candidate instanceof AbstractBeanDefinition) {
                    postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
                }
                                    
                if (checkCandidate(beanName, candidate)) {
                    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                    beanDefinitions.add(definitionHolder);
                    registerBeanDefinition(definitionHolder, this.registry);
                }
                
            }            
        }
        
        return beanDefinitions;
    }
    
    protected List<BeanDefinition> createFunctionBeanDefinitions(BeanDefinition candidate) {
        List<BeanDefinition> beanDefinitions = new ArrayList<BeanDefinition>();

        if (candidate instanceof AnnotatedBeanDefinition) {
            AnnotationMetadata metadata = ((AnnotatedBeanDefinition)candidate).getMetadata();
            if(!metadata.hasAnnotation(GemfireFunctionService.class.getCanonicalName())) {
                
                // GemfireFunctionService annotation missing, bean is a native function.
                // Change it's class to factory and set target
                String beanClassName = candidate.getBeanClassName();
                candidate.setBeanClassName(GEMFIREFUNCTIONFACTORYBEAN_CLASSNAME);
                candidate.getPropertyValues().add(BEAN_PROPERTY_TARGETCLASSNAME, beanClassName);
                beanDefinitions.add(candidate);
            } else {
                // create simple bean from candidate(will be referenced in proxy beans.
                beanDefinitions.add(candidate);
                String functionImplClassName = Introspector.decapitalize(ClassUtils.getShortName(candidate.getBeanClassName()));
                
                // Create function proxy bean defs from GemfireFunction annotated methods.
                Set<MethodMetadata> functionMethods = metadata.getAnnotatedMethods(GemfireFunction.class.getName());
                for (MethodMetadata methodMetadata : functionMethods) {
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClassName(GEMFIREFUNCTIONPROXYFACTORYBEAN_CLASSNAME);
                    beanDefinition.getPropertyValues().add(BEAN_PROPERTY_TARGET, new RuntimeBeanReference(functionImplClassName));
                    beanDefinition.getPropertyValues().add(BEAN_PROPERTY_METHODNAME, methodMetadata.getMethodName());
                    
                    // Function name either from annotation or method name
                    String id = (String) methodMetadata.getAnnotationAttributes(GemfireFunction.class.getCanonicalName()).get("id");
                    id = StringUtils.hasText(id) ? id : methodMetadata.getMethodName();
                    beanDefinition.getPropertyValues().add(BEAN_PROPERTY_FUNCTIONNAME, id);
                    beanDefinitions.add(beanDefinition);
                }
                
                // Create function execute proxy bean def from GemfireFunctionExecute annotated methods.
                // We only need to create one bean for classes having multiple annotated methods.
                Set<MethodMetadata> executeMethods = metadata.getAnnotatedMethods(GemfireFunctionExecute.class.getName());
                if(!executeMethods.isEmpty()) {
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClassName(GEMFIREFUNCTIONEXECUTEPROXYFACTORYBEAN_CLASSNAME);
                    beanDefinition.getPropertyValues().add(BEAN_PROPERTY_TARGET, new RuntimeBeanReference(functionImplClassName));
                    beanDefinition.getPropertyValues().add(BEAN_PROPERTY_CACHE, new RuntimeBeanReference("gemfire-cache"));
                    beanDefinitions.add(beanDefinition);                                        
                }
                
            }
            
        }
                
        return beanDefinitions;
    }
    
    /**
     * Generates unique bean name from base bean definition which
     * class is a common factory class and thus would not result
     * pre-determined bean name.
     * 
     * @param definition bean definition
     * @return unique name
     */
    protected String generateBeanName(BeanDefinition definition) {
        String baseName = ClassUtils.getShortName(definition.getBeanClassName());
        
        if(baseName.startsWith(GEMFIREFUNCTIONFACTORYBEAN_SIMPLE_CLASSNAME)) {
            baseName = baseName + "-" + ClassUtils.getShortName((String) definition.getPropertyValues().getPropertyValue("targetClassName").getValue());
        } else if(baseName.startsWith(GEMFIREFUNCTIONPROXYFACTORYBEAN_SIMPLE_CLASSNAME)) {            
            baseName = baseName + "-" + ClassUtils.getShortName((String) definition.getPropertyValues().getPropertyValue("methodName").getValue());
        } else if(baseName.startsWith(GEMFIREFUNCTIONEXECUTEPROXYFACTORYBEAN_SIMPLE_CLASSNAME)) {
            RuntimeBeanReference ref = (RuntimeBeanReference) definition.getPropertyValues().getPropertyValue("target").getValue();
            baseName = baseName + "-" + ref.getBeanName();
        }
        return Introspector.decapitalize(baseName);
    }

    /**
     * Register the specified bean with the given registry.
     * <p>Can be overridden in subclasses, e.g. to adapt the registration
     * process or to register further bean definitions for each scanned bean.
     * 
     * @param definitionHolder the bean definition plus bean name for the bean
     * @param registry the BeanDefinitionRegistry to register the bean with
     */
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

    /**
     * Apply further settings to the given bean definition,
     * beyond the contents retrieved from scanning the component class.
     * 
     * @param beanDefinition the scanned bean definition
     * @param beanName the generated bean name for the given bean
     */
    protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        beanDefinition.applyDefaults(this.beanDefinitionDefaults);
        if (this.autowireCandidatePatterns != null) {
            beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
        }        
    }

    /**
     * Check the given candidate's bean name, determining whether the corresponding
     * bean definition needs to be registered or conflicts with an existing definition.
     * 
     * @param beanName the suggested name for the bean
     * @param beanDefinition the corresponding bean definition
     * @return <code>true</code> if the bean can be registered as-is;
     *         <code>false</code> if it should be skipped because there is an
     *         existing, compatible bean definition for the specified name
     * @throws ConflictingBeanDefinitionException if an existing, incompatible
     *         bean definition has been found for the specified name
     */
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        if (!this.registry.containsBeanDefinition(beanName)) {
            return true;
        }
        BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
        BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
        if (originatingDef != null) {
            existingDef = originatingDef;
        }
        if (isCompatible(beanDefinition, existingDef)) {
            return false;
        }
        // ConflictingBeanDefinitionException
        throw new IllegalStateException("Annotation-specified bean name '" + beanName +
                "' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
                "non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
    }

    /**
     * Determine whether the given new bean definition is compatible with
     * the given existing bean definition.
     * <p>The default implementation simply considers them as compatible
     * when the bean class name matches.
     * @param newDefinition the new bean definition, originated from scanning
     * @param existingDefinition the existing bean definition, potentially an
     * explicitly defined one or a previously generated one from scanning
     * @return whether the definitions are considered as compatible, with the
     * new definition to be skipped in favor of the existing definition
     */
    protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
        return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
                newDefinition.getSource().equals(existingDefinition.getSource()) ||  // scanned same file twice
                newDefinition.equals(existingDefinition));  // scanned equivalent class twice
    }

    /**
     * Get the Environment from the given registry if possible, otherwise return a new
     * StandardEnvironment.
     */
    private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry instanceof EnvironmentCapable) {
            return ((EnvironmentCapable) registry).getEnvironment();
        }
        return new StandardEnvironment();
    }

}
