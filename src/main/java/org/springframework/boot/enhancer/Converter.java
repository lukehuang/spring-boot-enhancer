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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.boot.type.classreading.ConcurrentReferenceCachingMetadataReaderFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Base64Utils;

import net.bytebuddy.description.type.TypeDescription;

/**
 * @author Dave Syer
 *
 */
public class Converter {

	private ConcurrentReferenceCachingMetadataReaderFactory factory = new ConcurrentReferenceCachingMetadataReaderFactory(
			getClass().getClassLoader());

	private static Kryo kryo = new Kryo();

	static {
		kryo.addDefaultSerializer(ClassLoader.class,
				new SimpleMetadataReaderSerializer());
	}

	public static AnnotationMetadata deserialize(MetadataProvider provider) {
		try (Input stream = new Input(Base64Utils.decodeFromString(provider.raw()))) {
			AnnotationMetadata metadata = (AnnotationMetadata) kryo
					.readClassAndObject(stream);
			return metadata;
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not deserialize", e);
		}
	}

	public String serialize(TypeDescription typeDescription) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Output stream = new Output(bytes)) {
			factory.getMetadataReader(typeDescription.getActualName())
					.getAnnotationMetadata();
			kryo.writeClassAndObject(stream,
					factory.getMetadataReader(typeDescription.getActualName())
							.getAnnotationMetadata());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return Base64Utils.encodeToString(bytes.toByteArray());
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