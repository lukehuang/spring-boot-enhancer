package org.springframework.boot.enhancer;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.type.classreading.ConcurrentReferenceCachingMetadataReaderFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

/**
 * @author Dave Syer
 *
 */

public class DemoInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(new SpecialPostProcessor());
	}
}

class SpecialPostProcessor
		implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

	public static final String BEAN_NAME = "org.springframework.boot.autoconfigure."
			+ "internalCachingMetadataReaderFactory";

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 100;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {
		RootBeanDefinition definition = new RootBeanDefinition(
				FasterMetadataReaderFactoryBean.class);
		registry.registerBeanDefinition(BEAN_NAME, definition);
	}
}

class FasterMetadataReaderFactoryBean
		implements FactoryBean<ConcurrentReferenceCachingMetadataReaderFactory>,
		BeanClassLoaderAware, ApplicationListener<ContextRefreshedEvent> {

	private ConcurrentReferenceCachingMetadataReaderFactory metadataReaderFactory;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.metadataReaderFactory = new FasterMetadataReaderFactory(classLoader);
	}

	@Override
	public ConcurrentReferenceCachingMetadataReaderFactory getObject() throws Exception {
		return this.metadataReaderFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return CachingMetadataReaderFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.metadataReaderFactory.clearCache();
	}
}

class FasterMetadataReaderFactory
		extends ConcurrentReferenceCachingMetadataReaderFactory {

	public FasterMetadataReaderFactory() {
		super();
	}

	public FasterMetadataReaderFactory(ClassLoader classLoader) {
		super(classLoader);
	}

	public FasterMetadataReaderFactory(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	@Override
	protected MetadataReader createMetadataReader(Resource resource) throws IOException {
		if (resource instanceof ClassPathResource) {
			return serializableReader((ClassPathResource) resource);
		}
		return super.createMetadataReader(resource);
	}

	private MetadataReader serializableReader(ClassPathResource resource) {
		try {
			MetadataReader reader = super.createMetadataReader(resource);
			return reader;
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not serialize", e);
		}
	}
}