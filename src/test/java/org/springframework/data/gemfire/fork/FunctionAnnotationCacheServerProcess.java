package org.springframework.data.gemfire.fork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

import org.springframework.data.gemfire.ForkUtil;
import org.springframework.data.gemfire.function.AddMeasurementTicksFunction;
import org.springframework.data.gemfire.function.DeleteRegionEntriesFunction;
import org.springframework.data.gemfire.function.MeasurementsByTagsFunction;
import org.springframework.data.gemfire.function.RegionEntryCountFunction;
import org.springframework.data.gemfire.repository.samplefunction.Measurement;
import org.springframework.data.gemfire.repository.samplefunction.Measurements;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.RegionFactory;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.distributed.DistributedSystem;

public class FunctionAnnotationCacheServerProcess {
    
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("name", "ClientAnnotationServer");
        props.setProperty("log-level", "config");

        System.out.println("\nConnecting to the distributed system and creating the cache.");        
        Cache cache = new CacheFactory(props).create();
        
        RegionFactory regionFactory = cache.createRegionFactory(RegionShortcut.PARTITION);

        Region measurementsRegion = regionFactory.create("Measurements");
        
        System.out.println("Measurements region, " + measurementsRegion.getFullPath() + ", created in cache.");
        
        CacheServer server = cache.addCacheServer();
        server.setPort(40404);
        server.start(); 
        System.out.println("Server started");

        FunctionService.registerFunction(new MeasurementsByTagsFunction());
        FunctionService.registerFunction(new AddMeasurementTicksFunction());
        FunctionService.registerFunction(new DeleteRegionEntriesFunction());
        FunctionService.registerFunction(new RegionEntryCountFunction());
        
        ForkUtil.createControlFile(FunctionAnnotationCacheServerProcess.class.getName());
        
        System.out.println("Waiting for shutdown");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        bufferedReader.readLine();
    }

}
