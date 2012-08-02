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
package org.springframework.data.gemfire.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.gemstone.gemfire.cache.execute.ResultCollector;

/**
 * Annotation defining gemfire function usage.
 * 
 * @author Janne Valkealahti
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GemfireRepositoryFunction {
    
    /** Function name */
    String id();
    
    /** Target where function is executed. */
    FunctionTarget target() default FunctionTarget.ON_REGION;
    
    /** Class implementing gemfire result collector */
    Class<? extends ResultCollector<?, ?>> collector() default DefaultResultCollector.class;

    /** Value identifying region or pool depending on a set target */
    String value() default "";

    /** Parameter to treated as functino filter. */
    int filter() default -1;

    /** Function execution timeout. */
    long timeout() default -1;

    /** List of distibuted member names. */
    String[] members() default "";
}
