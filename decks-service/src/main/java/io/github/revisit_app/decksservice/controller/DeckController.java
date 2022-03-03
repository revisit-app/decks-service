package io.github.revisit_app.decksservice.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.revisit_app.decksservice.handler.DeckHandler;
import io.github.revisit_app.decksservice.handler.SavedDecksHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class DeckController {

  private final DeckHandler dh;
  private final SavedDecksHandler sdh;

  @Bean
  RouterFunction<ServerResponse> routerFunction() {

    return RouterFunctions.route()
        .POST(dh::handleCreateDeck)
        .DELETE("/{id}/cards", dh::handleRemoveCard)
        .DELETE("/saved/{id}", sdh::handleRemoveDeck)
        .DELETE("/{id}", dh::handleDeleteDeck)
        .PUT("/saved/{id}", sdh::handleAddDeck)
        .PUT("/{id}/cards", dh::handleAddCard)
        .PUT("/{id}", dh::handleUpdateDeck)
        .GET("/saved", sdh::handleGetSavedDecks)
        .GET("/{id}", dh::handleGetDeck)
        .build();
  }
}
