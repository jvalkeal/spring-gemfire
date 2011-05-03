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

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean.EvictionPolicy;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.gemstone.gemfire.cache.Region;

/**
 * Parser for <code>CacheServer</code> definitions.
 * 
 * @author vinesh (manoharanv at vmware dot com)
 * 
 */
class CacheServerParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(final Element element) {
		return CacheServerFactoryBean.class;
	}

	@Override
	protected String resolveId(final Element element,
			final AbstractBeanDefinition definition,
			final ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String name = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(name)) {
			name = "gemfire-cacheserver";
		}
		return name;
	}

	@Override
	protected boolean isEligibleAttribute(final Attr attribute,
			final ParserContext parserContext) {
		/*
		 * Suppress cache-ref element as we want to add it later as a property
		 * reference. By default AbstractSimpleBeanDefinitionParser considers
		 * all attributes as property values.
		 */
		return (attribute != null)
				&& (!"cache-ref".equals(attribute.getLocalName()))
				&& super.isEligibleAttribute(attribute, parserContext);
	}

	@Override
	protected void postProcess(final BeanDefinitionBuilder builder,
			final Element element) {
		// defensive guard
		if (element == null || builder == null) {
			return;
		}

		String attr = element.getAttribute("cache-ref");
		// add cache reference (fallback to default if nothing is specified)
		builder.addPropertyReference("cache", (StringUtils.hasText(attr) ? attr
				: "gemfire-cache"));

		// parse and handle server groups if available
		this.parseServerGroups(builder, element);

		// parse and handle client subscription queue if available.
		this.parseClientSubscription(builder, element);

	}

	private boolean parseClientSubscription(
			final BeanDefinitionBuilder builder, final Element element) {
		boolean parsingOccured = false;

		final Element clientSubscriptionElement = DomUtils
				.getChildElementByTagName(element, "client-subscription-config");

		if (clientSubscriptionElement == null) {
			return parsingOccured;
		}

		ParsingUtils.setPropertyValue(clientSubscriptionElement, builder,
				"eviction-type", "evictionPolicy");
		ParsingUtils.setPropertyValue(clientSubscriptionElement, builder,
				"eviction-capacity", "evictionCapacity");

		if (ConcurrentMap.class.isAssignableFrom(Region.class)) {
			/*
			 * Post 6.5.x only either disk store or overflow directory can be
			 * used. If disk store is available then prioritize that over
			 * overflow directory. Only if disk store is not available then
			 * attempt to parse overflow directory
			 */
			boolean diskStoreAvailable = parseDiskStore(builder,
					clientSubscriptionElement);
			boolean overFlowDirAvailable = false;
			if (!diskStoreAvailable) {
				overFlowDirAvailable = parseOverFlowDirectory(builder,
						clientSubscriptionElement);
			}

			parsingOccured = parsingOccured || diskStoreAvailable
					|| overFlowDirAvailable;
		} else {
			/*
			 * Prior to 6.5 disk store not support so ignore that only consider
			 * overflow-directory
			 */
			parsingOccured = parseOverFlowDirectory(builder,
					clientSubscriptionElement) || parsingOccured;
		}

		return parsingOccured;
	}

	private boolean parseDiskStore(final BeanDefinitionBuilder builder,
			final Element element) {
		boolean parsingOccured = false;
		final Element diskStoreElement = DomUtils.getChildElementByTagName(
				element, "disk-store");
		if (diskStoreElement != null) {
			ParsingUtils.setPropertyValue(diskStoreElement, builder,
					"disk-store-name", "diskStoreName");
			parsingOccured = true;
		}
		return parsingOccured;
	}

	private boolean parseOverFlowDirectory(final BeanDefinitionBuilder builder,
			final Element element) {
		boolean parsingOccured = false;
		final Element overFlowDirectoryElement = DomUtils
				.getChildElementByTagName(element, "overflow-directory");
		if (overFlowDirectoryElement != null) {
			ParsingUtils.setPropertyValue(overFlowDirectoryElement, builder,
					"directory-path", "overFlowDirectory");
			parsingOccured = true;
		}
		return parsingOccured;
	}

	private boolean parseServerGroups(final BeanDefinitionBuilder builder,
			final Element element) {
		boolean parsingOccured = false;

		final Element serverGroupsElement = DomUtils.getChildElementByTagName(
				element, "servergroups");

		if (serverGroupsElement != null) {
			final List<Element> serverGroupElements = DomUtils
					.getChildElementsByTagName(serverGroupsElement,
							"servergroup");

			final ManagedList<String> serverGroups = new ManagedList<String>();
			for (Element subElement : serverGroupElements) {
				String attr = subElement.getAttribute("groupname");
				if (StringUtils.hasText(attr)) {
					serverGroups.add(attr);
				}
			}

			if (!serverGroups.isEmpty()) {
				builder.addPropertyValue("serverGroups", serverGroups);
				parsingOccured = true;
			}
		}

		return parsingOccured;
	}
}
