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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Repository post processor to add advice to repository methods to
 * intercept distributed locking.
 * 
 * @author Janne Valkealahti
 *
 */
public class DistibutedLockRepositoryPostProcessor implements RepositoryProxyPostProcessor {
    
    private final static Log log = LogFactory.getLog(DistibutedLockRepositoryPostProcessor.class);

    /** Central locking service */
    private GemfireDistributedLockService gemfireDistributedLockService;
    
    /** Default service name */
    private String serviceName;
      
    /**
     * 
     * @param gemfireDistributedLockService
     * @param serviceName
     */
    public DistibutedLockRepositoryPostProcessor(GemfireDistributedLockService gemfireDistributedLockService, String serviceName) {
        super();
        this.gemfireDistributedLockService = gemfireDistributedLockService;
        this.serviceName = serviceName;
    }

    @Override
    public void postProcess(ProxyFactory factory) {
        factory.addAdvice(new DistibutedLockPopulatingMethodIntercceptor());
    }
    
    /**
     * 
     */
    public class DistibutedLockPopulatingMethodIntercceptor implements MethodInterceptor, AfterReturningAdvice, ThrowsAdvice {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            DistibutedLock annotation = method.getAnnotation(DistibutedLock.class);
            
            // locking not requested
            if(annotation == null) {
                return invocation.proceed();
            }
            
            DistributedLockPolicy policy = annotation.policy();
            
            if(policy.equals(DistributedLockPolicy.NONE)) {
                // locking not requested
                return invocation.proceed();                
            } 
            
            long waitMillis = annotation.waitTimeMillis();
            long leaseMillis = annotation.leaseTimeMillis();

            String lockServiceName = resolveServiceName(annotation);
            Object lockObj = null;

            if (policy.equals(DistributedLockPolicy.CUSTOM)) {
                lockObj = annotation.lockBy().isEmpty() ? "defaultGemfireRepositoryLockObj" : annotation.lockBy();
            } else if (policy.equals(DistributedLockPolicy.METHOD)) {
                lockObj = method.toString();
            } else if (policy.equals(DistributedLockPolicy.PARAMETER)) {
                lockObj = invocation.getArguments()[annotation.parameter()];
            }

            gemfireDistributedLockService.lock(lockServiceName,lockObj, waitMillis, leaseMillis);
            TransactionSynchronizationManager.bindResource(method, lockObj);

            return invocation.proceed();
        }
        
        private String resolveServiceName(DistibutedLock annotation) {
            String lockServiceName = annotation.serviceName();
            if(lockServiceName.isEmpty()) {
                lockServiceName = serviceName;
            }
            return lockServiceName;
        }

        @Override
        public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
            releaseLock(method);
        }
                
        public void afterThrowing(Method method, Object[] args, Object target, Exception e) {
            // clear lock in case exception is thrown. 
            releaseLock(method);
        }
        
        private void releaseLock(Method method) {
            Object lockObj = TransactionSynchronizationManager.getResource(method);
            if(lockObj != null) {
                DistibutedLock annotation = method.getAnnotation(DistibutedLock.class);
                String lockServiceName = resolveServiceName(annotation);
                gemfireDistributedLockService.unlock(lockServiceName, lockObj);
                TransactionSynchronizationManager.unbindResource(method);                
            }                        
        }
        
    }

}
