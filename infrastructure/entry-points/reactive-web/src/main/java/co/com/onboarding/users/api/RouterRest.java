package co.com.onboarding.users.api;

import co.com.onboarding.users.api.error.ApiErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler, ApiErrorHandler apiErrorHandler) {
        return route(PUT("/api/v1/users/{id}"), handler::createUser)
                .filter(apiErrorHandler);
    }
}
