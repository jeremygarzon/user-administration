package co.com.onboarding.users.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_PATH = USERS_PATH + "/{id}";

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(GET(USERS_PATH), handler::listUsers)
                .andRoute(POST(USERS_PATH), handler::createUser)
                .andRoute(GET(USER_PATH), handler::getUser);
    }
}
