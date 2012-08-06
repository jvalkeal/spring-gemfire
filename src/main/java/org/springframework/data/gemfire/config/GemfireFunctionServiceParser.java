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
package org.springframework.data.gemfire.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.gemfire.function.GemfireFunctionService;
import org.springframework.data.gemfire.function.support.FunctionBeanDefinitionScanner;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.gemstone.gemfire.cache.execute.Function;

/**
 * Namespace parser for function-service tag.
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionServiceParser implements BeanDefinitionParser {

    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";
    private static final String SCAN_INTERFACE_ATTRIBUTE = "scan-interface";
    private static final String SCAN_ANNOTATED_ATTRIBUTE = "scan-annotated";
    private static final String CACHE_REF_ATTRIBUTE = "cache-ref";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {

        String[] basePackages = StringUtils.tokenizeToStringArray(element.getAttribute(BASE_PACKAGE_ATTRIBUTE),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        List<String> functionClasses = new ArrayList<String>();
        List<Element> functionDefs = DomUtils.getChildElementsByTagName(element, "function");
        for (Element functionDef : functionDefs) {
            String className = functionDef.getAttribute("class");
            if(StringUtils.hasText(className)) {
                functionClasses.add(className);                
            }
        }

        FunctionBeanDefinitionScanner scanner = configureScanner(parserContext, element);
        Set<BeanDefinitionHolder> beanDefinitions = scanner.doScan(functionClasses, basePackages);
        
        registerComponents(parserContext.getReaderContext(), beanDefinitions, element);

        return null;
    }
    
    protected FunctionBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        XmlReaderContext readerContext = parserContext.getReaderContext();

        boolean scanInterfaces = true;
        boolean scanAnnotations = true;
        
        if (element.hasAttribute(SCAN_INTERFACE_ATTRIBUTE)) {
            scanInterfaces = Boolean.valueOf(element.getAttribute(SCAN_INTERFACE_ATTRIBUTE));
        }

        if (element.hasAttribute(SCAN_ANNOTATED_ATTRIBUTE)) {
            scanAnnotations = Boolean.valueOf(element.getAttribute(SCAN_ANNOTATED_ATTRIBUTE));
        }
        
        FunctionBeanDefinitionScanner scanner = createScanner(readerContext);
        scanner.setCacheRefBeanName(element.getAttribute(CACHE_REF_ATTRIBUTE));
        
        if(scanInterfaces) {
            scanner.addIncludeFilter(new AssignableTypeFilter(Function.class));            
        }
        
        if(scanAnnotations) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(GemfireFunctionService.class));            
        }
        
        return scanner;
    }

    protected void registerComponents(XmlReaderContext readerContext, Set<BeanDefinitionHolder> beanDefinitions, Element element) {

        Object source = readerContext.extractSource(element);
        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);

        for (BeanDefinitionHolder beanDefHolder : beanDefinitions) {
            compositeDef.addNestedComponent(new BeanComponentDefinition(beanDefHolder));
        }

        readerContext.fireComponentRegistered(compositeDef);
    }

    protected FunctionBeanDefinitionScanner createScanner(XmlReaderContext readerContext) {
        return new FunctionBeanDefinitionScanner(readerContext.getRegistry());
    }
    
}
