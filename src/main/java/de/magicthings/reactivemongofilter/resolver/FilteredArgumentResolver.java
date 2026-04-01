package de.magicthings.reactivemongofilter.resolver;

import de.magicthings.reactivemongofilter.annotation.Filtered;
import de.magicthings.reactivemongofilter.model.FilterRequest;
import de.magicthings.reactivemongofilter.parser.FilterRequestParser;
import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class FilteredArgumentResolver implements HandlerMethodArgumentResolver {

    private final ApplicationContext applicationContext;
    private final FilterRequestParser filterRequestParser;

    public FilteredArgumentResolver(ApplicationContext applicationContext, FilterRequestParser filterRequestParser) {
        this.applicationContext = applicationContext;
        this.filterRequestParser = filterRequestParser;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Filtered.class)
                && FilterRequest.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        Filtered annotation = parameter.getParameterAnnotation(Filtered.class);
        EntityFilterSpec<?> spec = applicationContext.getBean(annotation.value());
        FilterRequest filterRequest = filterRequestParser.parse(exchange.getRequest().getQueryParams(), spec);
        return Mono.just(filterRequest);
    }
}