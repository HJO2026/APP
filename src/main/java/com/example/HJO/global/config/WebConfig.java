package com.example.HJO.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.HJO.domain.PostSort;
import com.example.HJO.global.auth.CurrentUserIdArgumentResolver;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentUserIdArgumentResolver());
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		// ?sort=latest|popular (대소문자 무시). 변환 실패는 400
		registry.addConverter(String.class, PostSort.class, (Converter<String, PostSort>) PostSort::from);
	}

}
