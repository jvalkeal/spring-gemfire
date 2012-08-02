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
package org.springframework.data.gemfire.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.gemfire.function.support.GemfireFunctionServiceFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Namespace parser for function-service tag.
 * 
 * @author Janne Valkealahti
 */
public class GemfireFunctionServiceParser extends AbstractSimpleBeanDefinitionParser {
       
    @Override
    protected Class<GemfireFunctionServiceFactoryBean> getBeanClass(Element element) {
        return GemfireFunctionServiceFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        ParsingUtils.setPropertyValue(element, builder, "base-package", "basePackage");
        ParsingUtils.setPropertyValue(element, builder, "scan-interface", "scanInterface");
        ParsingUtils.setPropertyValue(element, builder, "scan-annotated", "scanAnnotated");

        List<Element> listFunctionDefs = DomUtils.getChildElementsByTagName(element, "function");

        if (!listFunctionDefs.isEmpty()) {
            List<String> functions = new ArrayList<String>();
            for (Element listElement : listFunctionDefs) {
                functions.add(listElement.getAttribute("class"));
            }
            builder.addPropertyValue("functions", functions);
        }

    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
            throws BeanDefinitionStoreException {
        String name = super.resolveId(element, definition, parserContext);
        if (!StringUtils.hasText(name)) {
            name = "gemfire-function-service";
        }
        return name;
    }

}
