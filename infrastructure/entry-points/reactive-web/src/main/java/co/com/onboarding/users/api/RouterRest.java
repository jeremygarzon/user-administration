package co.com.onboarding.users.api;

import co.com.onboarding.users.api.error.ApiErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_PATH = USERS_PATH + "/{id}";

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler, ApiErrorHandler apiErrorHandler) {
        return route(GET(USERS_PATH), handler::listUsers)
                .andRoute(GET(USER_PATH), handler::getUser)
                .andRoute(PUT(USER_PATH), handler::createUser)
                .filter(apiErrorHandler);
    }
}
