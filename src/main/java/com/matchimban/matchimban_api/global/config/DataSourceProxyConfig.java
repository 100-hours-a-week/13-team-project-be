package com.matchimban.matchimban_api.global.config;

import com.matchimban.matchimban_api.global.logging.SqlMetricsQueryListener;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceProxyConfig {

	@Bean
	public static BeanPostProcessor dataSourceProxyBeanPostProcessor(
		ObjectProvider<SqlMetricsQueryListener> listenerProvider
	) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (!(bean instanceof DataSource dataSource)) {
					return bean;
				}

				SqlMetricsQueryListener listener = listenerProvider.getIfAvailable();
				if (listener == null) {
					return bean;
				}

				return ProxyDataSourceBuilder.create(dataSource)
					.name(beanName)
					.listener(listener)
					.build();
			}
		};
	}
}
