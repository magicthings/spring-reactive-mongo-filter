package de.magicthings.reactivemongofilter.config;

import de.magicthings.reactivemongofilter.parser.FilterCriteriaBuilder;
import de.magicthings.reactivemongofilter.parser.FilterRequestParser;
import de.magicthings.reactivemongofilter.repository.ReactiveFilterRepository;
import de.magicthings.reactivemongofilter.resolver.FilteredArgumentResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
@ConditionalOnClass(ReactiveMongoTemplate.class)
public class ReactiveMongoFilterAutoConfiguration implements WebFluxConfigurer {

    private final ApplicationContext applicationContext;

    public ReactiveMongoFilterAutoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRequestParser filterRequestParser() {
        return new FilterRequestParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterCriteriaBuilder filterCriteriaBuilder() {
        return new FilterCriteriaBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveFilterRepository reactiveFilterRepository(
            ReactiveMongoTemplate mongoTemplate,
            FilterCriteriaBuilder criteriaBuilder
    ) {
        return new ReactiveFilterRepository(mongoTemplate, criteriaBuilder);
    }

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new FilteredArgumentResolver(applicationContext, filterRequestParser()));
    }
}