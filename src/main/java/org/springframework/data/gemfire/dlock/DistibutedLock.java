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
package org.springframework.data.gemfire.dlock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for distributed lock.
 * 
 * @author Janne Valkealahti
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistibutedLock {
    
    /**
     * Distributed lock service name.
     */
    String serviceName() default "";

    /**
     * Custom string used as lock object if policy is set
     * to DistributedLockPolicy.CUSTOM.
     */
    String lockBy() default "";

    /**
     * Wait time passed to lock function.
     */
    long waitTimeMillis() default -2;

    /**
     * Lease time passed to lock function.
     */
    long leaseTimeMillis() default -2;

    /**
     * Parameter to be used as lock object if policy is set
     * to DistributedLockPolicy.PARAMETER.
     */
    int parameter() default -1;

    /**
     * Defines locking policy.
     */
    DistributedLockPolicy policy() default DistributedLockPolicy.METHOD;
    
}
