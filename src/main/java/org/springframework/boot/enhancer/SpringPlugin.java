/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.boot.enhancer;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * @author Dave Syer
 *
 */
public class SpringPlugin implements Plugin {

	private static Log logger = LogFactory.getLog(SpringPlugin.class);

	private Converter converter = new Converter();

	private static final String METADATA = "__ANNOTATION_METADATA__";

	@Override
	public boolean matches(TypeDescription target) {
		return target.getDeclaredAnnotations().stream().anyMatch(this::isConfiguration);
	}

	private boolean isConfiguration(AnnotationDescription desc) {
		return isConfiguration(desc, new HashSet<>());
	}

	private boolean isConfiguration(AnnotationDescription desc,
			Set<AnnotationDescription> seen) {
		seen.add(desc);
		TypeDescription type = desc.getAnnotationType();
		if (type.represents(Configuration.class)) {
			return true;
		}
		for (AnnotationDescription ann : type.getDeclaredAnnotations()) {
			if (!seen.contains(ann) && isConfiguration(ann, seen)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription) {
		logger.info("Instrumenting: " + typeDescription.getActualName());
		String value = converter.serialize(typeDescription);
		return builder
				.defineField(METADATA, String.class, Modifier.STATIC | Modifier.PUBLIC)
				.value(value)
				.method(ElementMatchers.isDeclaredBy(MetadataProvider.class)
						.and(ElementMatchers.named("raw")))
				.intercept(FieldAccessor.ofField(METADATA))
				.method(ElementMatchers.isDeclaredBy(MetadataProvider.class)
						.and(ElementMatchers.named("deserialize")))
				.intercept(MethodDelegation.to(Converter.class))
				.implement(MetadataProvider.class);
	}

}
