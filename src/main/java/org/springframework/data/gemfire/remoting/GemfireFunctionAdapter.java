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

import java.io.Serializable;

import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;
import com.gemstone.gemfire.cache.execute.ResultSender;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;

/**
 * Distributed function.
 * 
 * @author Costin Leau
 */
class GemfireFunctionAdapter implements Function {

	private final boolean ha, optimizeWrite, hasResult;

	GemfireFunctionAdapter(boolean ha, boolean optimizeWrite, boolean hasResult) {
		this.ha = ha;
		this.optimizeWrite = optimizeWrite;
		this.hasResult = hasResult;
	}

	public void execute(FunctionContext context) {
		Region<?, ?> region = null;

		logger.debug("exectuting delegate method [" + delegateClassName + "." + methodName + "]");
		if (context instanceof RegionFunctionContext) {
			RegionFunctionContext rc = (RegionFunctionContext) context;
			region = rc.getDataSet();
			if (PartitionRegionHelper.isPartitionedRegion(rc.getDataSet())) {
				region = PartitionRegionHelper.getLocalDataForContext(rc);
			}
		}

		Object instance = createDelegateInstance(region);

		Serializable result = invokeDelegateMethod(instance, context.getArguments());

		if (hasResult()) {
			logger.debug("result: " + result);
			sendResults(context.getResultSender(), result);
		}
	}

	private void sendResults(ResultSender<Serializable> resultSender, Serializable result) {
		if (result == null) {
			resultSender.lastResult(null);
			return;
		}

		Serializable lastItem = result;

		List<Serializable> results = null;
		if (ObjectUtils.isArray(result)) {
			results = Arrays.asList((Object[]) result);
		}
		else if (List.class.isAssignableFrom(result.getClass())) {
			results = (List<Serializable>) result;
		}

		if (results != null) {
			int i = 0;
			for (Serializable item : results) {
				if (i++ < results.size() - 1) {
					resultSender.sendResult(item);
				}
				else {
					lastItem = item;
				}
			}
		}
		resultSender.lastResult(lastItem);
	}

	protected final Serializable invokeDelegateMethod(Object instance, Serializable args) {
		// Null parameters returns any method signature 
		Method method = ReflectionUtils.findMethod(instance.getClass(), methodName, (Class<?>[]) null);
		if (method == null) {
			throw new FunctionException("cannot find method [" + methodName + "] on type ["
					+ instance.getClass().getName() + "]");
		}

		return (Serializable) ReflectionUtils.invokeMethod(method, instance, (Object[]) args);
	}

	public String getId() {
		return getClass().getName();
	}

	public boolean hasResult() {
		return hasResult;
	}

	public boolean isHA() {
		return ha;
	}

	public boolean optimizeForWrite() {
		return optimizeWrite;
	}
}