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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.gemfire.function.GemfireFunctionContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;

/**
 * Factory bean for creating a Gemfire function proxy.
 * 
 * <p>This factory can be used to create a proxy against a normal POJO method.
 * 
 * <p>Manually define bean definitions:
 * <pre>
 * &lt;bean id="customFunctions" class="org.springframework.data.gemfire.function.CustomFunctionsImpl"/>
 *
 * &lt;bean id="someProxyFunction" class="org.springframework.data.gemfire.function.support.GemfireFunctionProxyFactoryBean">
 *   &lt;property name="target" ref="customFunctions"/>
 *   &lt;property name="methodName" value="getRegionLocalSize1"/>
 *   &lt;property name="functionName" value="GetRegionLocalSizeFunction"/>
 *   &lt;property name="isHA" value="true"/>
 *   &lt;property name="hasResults" value="true"/>
 *   &lt;property name="optimizedForWrite" value="false"/>
 * &lt;/bean>
 * </pre>
 * 
 * <p>Function instance can be requested from application context:
 * <pre>
 * Function someProxyFunction = (Function)context.getBean("someProxyFunction");
 * </pre>
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionProxyFactoryBean implements FactoryBean<Function>, InitializingBean {
    
    private final static Log log = LogFactory.getLog(GemfireFunctionProxyFactoryBean.class);

    /** Function name, must be set. */
    private String functionName;
    
    /** Function isHA flag, defaults to true. */
    private boolean isHA;
    
    /** Function optimizedForWrite flag, defaults to false. */
    private boolean optimizedForWrite;
    
    /** Function hasResults flag, defaults to true. */
    private boolean hasResults;
    
    /** Search string for function method in target object. */
    private String methodName;
    
    /** Target object where function endpoint resides. */
    private Object target;
    
    /** 
     * Method for function. Either explicitly set or 
     * resolved from methodName.
     */
    private Method method;
    
    /** Proxy to a function returned by this factory. */
    private Function functionProxy;
    
    /**
     * Default constructor.
     */
    public GemfireFunctionProxyFactoryBean() {
        super();
        this.target = null;
        this.functionName = null;
        this.optimizedForWrite = false;
        this.isHA = true;
        this.hasResults = true;
        this.methodName = null;
        this.method = null;
    }

    @Override
    public Function getObject() throws Exception {
        return functionProxy;
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
        Assert.notNull(target, "Target object must be set");
        Assert.notNull(functionName, "Function name must be set");
        
        if(method == null) {
            Assert.notNull(methodName, "Method is not explicitly set, method signature must be set");
            method = BeanUtils.resolveSignature(this.methodName, target.getClass());            
        }
        
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(FunctionImpl.class, getClass().getClassLoader());
        FunctionInvocationHandler fih = new FunctionInvocationHandler(functionName, isHA, hasResults,
                optimizedForWrite, method, target, parameterAnnotations);        
        
        functionProxy = (Function)Proxy.newProxyInstance(getClass().getClassLoader(), ifcs, fih);
    }
    
    /**
     * Sets a registered name of the function created by this factory.
     * 
     * @param functionName the functionName to set
     */
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    /**
     * Sets a target method which will be proxied when function
     * is executed.
     * 
     * @param method the method to set
     */    
    public void setMethod(Method method) {
        this.method = method;
    }

    /**
     * Sets function {@literal isHA} flag.
     * Defaults to true.
     * 
     * @param isHA the isHA to set
     */
    public void setIsHA(boolean isHA) {
        this.isHA = isHA;
    }

    /**
     * Sets function {@literal optimizedForWrite} flag.
     * Defaults to false.
     * 
     * @param optimizedForWrite the optimizedForWrite to set
     */
    public void setOptimizedForWrite(boolean optimizedForWrite) {
        this.optimizedForWrite = optimizedForWrite;
    }

    /**
     * Sets function {@literal hasResults} flag.
     * Defaults to true.
     * 
     * @param hasResults the hasResults to set
     */
    public void setHasResults(boolean hasResults) {
        this.hasResults = hasResults;
    }

    /**
     * Sets a signature to find method from a target class. Optionally
     * it's possible to set method directly using {@link #setMethod(Method)}.
     * 
     * @param methodName the name of the {@link Method} to locate
     * @see org.springframework.beans.BeanUtils#resolveSignature(String, Class)
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    /**
     * Sets a target object where proxied function call
     * will executed.
     * 
     * @param target the target to set
     */
    public void setTarget(Object target) {
        this.target = target;
    }

    /**
     * Handled to intercept function execution. Rest of the method
     * calls are passed to stub object.
     */
    private class FunctionInvocationHandler implements InvocationHandler {
        
        FunctionImpl functionImpl;
        Method targetMethod; // function execute method
        Object targetObject; // object for function method
        Annotation[][] parameterAnnotations;
        
        FunctionInvocationHandler(String id, boolean ha, boolean results, boolean optimize, Method targetMethod,
                Object targetObject, Annotation[][] parameterAnnotations) {
            functionImpl = new FunctionImpl(id, ha, results, optimize);
            this.targetMethod = targetMethod;
            this.targetObject = targetObject;
            this.parameterAnnotations = parameterAnnotations;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getName().equals("execute")) {
                
                FunctionContext context = (FunctionContext)args[0];
                
                // pass context to method if requested
                // using GemfireFunctionContext annotation
                Class<?>[] parameterTypes = targetMethod.getParameterTypes();
                Object[] invokeArgs = new Object[parameterTypes.length];
                for(int i = 0; i<parameterTypes.length; i++) {
                    if(hasGemfireFunctionContextAnnotation(i)) {
                        if(parameterTypes[i] == RegionFunctionContext.class) {
                            invokeArgs[i] = (RegionFunctionContext)context;
                        } else if(parameterTypes[i] == FunctionContext.class) {
                            invokeArgs[i] = context;
                        } else {
                            invokeArgs[i] = null;
                        }
                    }
                }
                
                Object returned = targetMethod.invoke(targetObject, invokeArgs);
                
                // if method returns something(else than void),
                // handle it here through result sender
                log.info("XXXXX: " + returned + " / " +
                        targetMethod.toGenericString());
                if(returned != null) {
                    context.getResultSender().lastResult(returned);
                }
                                
                return null;
            }
            
            try {
                Object retVal = method.invoke(functionImpl, args);
                return retVal;
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        private <T extends Annotation> boolean hasGemfireFunctionContextAnnotation(int parameter) {
            if(parameterAnnotations.length < parameter && parameter >= 0) {
                return false;
            }
            Annotation[] annotations = parameterAnnotations[parameter];
            for (Annotation annotation : annotations) {
                if(annotation instanceof GemfireFunctionContext) {
                    return true;
                }
            }
            return false;
        }

    }
    
    /**
     * Stub function implementation where parameters are kept,
     * and execute handled in proxy to this function.
     */
    private class FunctionImpl implements Function {
       
        private static final long serialVersionUID = -7012988440525836947L;
        
        String id;
        boolean ha;
        boolean results;
        boolean optimize;

        public FunctionImpl(String id, boolean ha, boolean results, boolean optimize) {
            super();
            this.id = id;
            this.ha = ha;
            this.results = results;
            this.optimize = optimize;
        }

        @Override
        public void execute(FunctionContext context) {
            // stub, never executed, handled in proxy
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean hasResult() {
            return results;
        }

        @Override
        public boolean isHA() {
            return ha;
        }

        @Override
        public boolean optimizeForWrite() {
            return optimize;
        }
        
    }


}
