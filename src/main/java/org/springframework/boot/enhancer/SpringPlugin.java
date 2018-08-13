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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.Base64Utils;
import org.springframework.util.ClassUtils;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * @author Dave Syer
 *
 */
public class SpringPlugin implements Plugin {

	private static Log logger = LogFactory.getLog(SpringPlugin.class);

	private Kryo kryo = new Kryo();

	{
		kryo.addDefaultSerializer(ClassLoader.class,
				new SimpleMetadataReaderSerializer());
	}

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
		String value = getMetadata(typeDescription);
		return builder
				.defineField(METADATA, String.class, Modifier.STATIC | Modifier.PUBLIC)
				.value(value)
				.method(ElementMatchers.isDeclaredBy(Initializer.class)
						.and(ElementMatchers.named("metadata")))
				.intercept(FieldAccessor.ofField(METADATA)).implement(Initializer.class);
	}

	private String getMetadata(TypeDescription typeDescription) {
		Class<?> type = ClassUtils.resolveClassName(typeDescription.getActualName(),
				Configuration.class.getClassLoader());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Output stream = new Output(bytes);
		kryo.writeClassAndObject(stream, new StandardAnnotationMetadata(type));
		stream.close();
		String value = Base64Utils.encodeToString(bytes.toByteArray());
		return value;
	}

}

class SimpleMetadataReaderSerializer extends Serializer<ClassLoader> {

	@Override
	public void write(Kryo kryo, Output output, ClassLoader object) {
	}

	@Override
	public ClassLoader read(Kryo kryo, Input input, Class<ClassLoader> type) {
		return getClass().getClassLoader();
	}

}
