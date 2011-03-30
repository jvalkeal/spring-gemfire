/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.gemfire.remoting;

import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;

import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.execute.ResultCollector;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing a
 * GemFire-based remote service/function.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects
 *
 * @author Costin Leau
 */
public class FunctionInvokerClientInterceptor implements MethodInterceptor, InitializingBean {

	public void afterPropertiesSet() throws Exception {
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return "Gemfire function proxy for service interface [" + this.serviceInterface + "]";
		}

		logger.debug("invoking method " + invocation.getMethod().getName());

		if (isSetFilterMethod(invocation.getMethod())) {
			setFilter((Set<? extends Serializable>) invocation.getArguments()[0]);
			return getObject();
		}


		Execution execution = FunctionService.onRegion(region);
		if (getFilter() != null) {
			execution.withFilter(getFilter());
		}

		MethodInvokingFunction function = new MethodInvokingFunction(this.delegateClassName,
				invocation.getMethod().getName());
		overrideDefaultOptions(function);

		ResultCollector<?, ?> resultsCollector = execution.withArgs(invocation.getArguments()).execute(function);

		return extractResult(resultsCollector, invocation.getMethod().getReturnType());
	}

	private Object extractResult(ResultCollector<?, ?> resultsCollector, Class<?> returnType) throws FunctionException,
			InterruptedException {
		Object result = null;
		List<?> results = null;
		if (timeout <= 0) {
			results = (List<?>) resultsCollector.getResult();

		}
		else {
			results = ((List<?>) resultsCollector.getResult(timeout, TimeUnit.MILLISECONDS));
		}

		if (List.class.isAssignableFrom(returnType)) {
			result = results;
		}
		else {
			int nonNullItems = 0;
			for (Object obj : results) {
				if (obj != null) {
					if (++nonNullItems > 1) {
						throw new FunctionException("multiple results found for single valued return type");
					}
					else {
						result = obj;
					}
				}
			}
		}

		logger.debug("returning result as " + result.getClass().getName());
		return result;
	}
}