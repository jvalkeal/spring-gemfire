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

/**
 * Target enum defining where function is executed.
 * <br>
 * These enums follows methods in {@code com.gemstone.gemfire.cache.execute.Execution}.
 * 
 * @author Janne Valkealahti
 */
public enum FunctionTarget {
    
    // ** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onMember(DistributedSystem, DistributedMember)} */
    // Not yet supported
    //    ON_DS_MEMBER,
    
    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onMembers(DistributedSystem)} */
    ON_ALL_DS_MEMBERS,
    
    // ** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onMembers(DistributedSystem, Set<DistributedMember>)} */
    // Not yet supported
    //    ON_DS_MEMBERS,
    
    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onRegion(Region)} */
    ON_REGION,
    
    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onServer(Pool)} */
    ON_SERVER_POOL,

    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onServer(RegionService)} */
    ON_SERVER_CACHE,

    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onServers(Pool)} */
    ON_SERVERS_POOL,
    
    /** Routing to {@code com.gemstone.gemfire.cache.execute.FunctionService.onServers(RegionService)} */
    ON_SERVERS_CACHE

}
