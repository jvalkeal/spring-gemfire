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
package org.springframework.data.gemfire.repository.function;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.gemfire.function.Function;
import org.springframework.data.gemfire.function.FunctionTarget;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionMethod extends QueryMethod {
    
    private final Method method;
    
    public GemfireFunctionMethod(Method method, RepositoryMetadata metadata) {
        super(method, metadata);
        this.method = method;
    }
    
    public boolean hasAnnotatedFunction() {
        return StringUtils.hasText(getFunctionId());
    }

    public String getFunctionId() {
        Function function = method.getAnnotation(Function.class);
        String functionIdString = function == null ? null : (String) AnnotationUtils.getValue(function, "id");
        return StringUtils.hasText(functionIdString) ? functionIdString : null;
    }
    
    public FunctionTarget getFunctionTarget() {
        Function function = method.getAnnotation(Function.class);
        if(function != null) {
            return function.target();
        }
        return null;            
    }
    
    public String getFunctionValue() {
        Function function = method.getAnnotation(Function.class);
        if(function != null) {
            return function.value();
        }
        return null;            
    }

    public Class<?> getFunctionCollector() {
        Function function = method.getAnnotation(Function.class);
        return function.collector();
    }
    
    public int getFunctionFilter() {
        Function function = method.getAnnotation(Function.class);
        return function.filter();
    }
    
    public long getFunctionTimeout() {
        Function function = method.getAnnotation(Function.class);
        return function.timeout();    
    }
    
    public String[] getFunctionMembers() {
        Function function = method.getAnnotation(Function.class);
        if(function != null) {
            return function.members();
        }
        return null;                    
    }

}
