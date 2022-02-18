package io.github.revisit_app.decksservice.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.revisit_app.decksservice.handler.DeckHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class DeckController {

  private final DeckHandler dh;

  @Bean
  RouterFunction<ServerResponse> routerFunction() {

    return RouterFunctions.route()
                .POST(dh::handleCreateDeck)
                .DELETE("/{id}", dh::handleDeleteDeck)
                .PUT("/{id}", dh::handleUpdateDeck)
                .GET("/{id}", dh::handleGetDeck)
                .build();
  }
}
