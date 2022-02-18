package io.github.revisit_app.decksservice.handler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.revisit_app.decksservice.entity.Deck;
import io.github.revisit_app.decksservice.entity.UserUDT;
import io.github.revisit_app.decksservice.repository.DeckRepo;
import io.github.revisit_app.decksservice.util.DeckData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckHandler {

  private final DeckRepo dr;

  public Mono<ServerResponse> handleGetDeck(ServerRequest request) {
    var deckId = request.pathVariable("id");
    log.info("Getting deck: {}", deckId);
    var response = dr.findById(deckId)
        .flatMap(card -> ServerResponse.ok().bodyValue(card))
        .switchIfEmpty(ServerResponse.notFound().build());

    return response;
  }

  public Mono<ServerResponse> handleCreateDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank())
      return ServerResponse.badRequest().build();
    var userId = request.headers().header("userId").get(0);
    var response = request.bodyToMono(DeckData.class).flatMap(cd -> {
      // Fetch user details from user service
      UserUDT author = new UserUDT(Long.valueOf(userId),
          "zaid",
          "Zaid",
          "Sheikh");
      Deck deck = new Deck(
          UUID.randomUUID().toString(),
          author,
          cd.getTitle(),
          cd.getDesc(),
          Instant.now(),
          Instant.now(),
          0,
          0,
          0,
          new ArrayList<>(),
          new ArrayList<>());

      log.info("Creating deck: {}", deck.getId());

      return dr.save(deck).flatMap(d -> ServerResponse.created(URI.create(request.uri() + deck.getId())).build());
    });

    return response;
  }

  public Mono<ServerResponse> handleUpdateDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank())
      return ServerResponse.badRequest().build();
    long userId = Long.valueOf(request.headers().header("userId").get(0));
    var deckId = request.pathVariable("id");
    var response = request.bodyToMono(DeckData.class)
        .flatMap(data -> dr.findById(deckId)
            .flatMap(od -> {
              if (od.getAuthor().getId() != userId)
                return ServerResponse.status(HttpStatus.FORBIDDEN).build();
              else {
                od.setTitle(data.getTitle());
                od.setDesc(data.getDesc());
                od.setDateUpdated(Instant.now());
                log.info("Updating deck: {}", od.getId());

                return dr.save(od)
                    .flatMap(nd -> ServerResponse.created(URI.create(request.uri().toString()))
                    .build());
              }
            }))
        .switchIfEmpty(ServerResponse.notFound().build());

    return response;
  }

  public Mono<ServerResponse> handleDeleteDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank())
      return ServerResponse.badRequest().build();
    var deckId = request.pathVariable("id");
    long userId = Long.valueOf(request.headers().header("userId").get(0));
    var response = dr.findById(deckId)
    .flatMap(deck -> {
      if(deck.getAuthor().getId() != userId)
        return ServerResponse.status(HttpStatus.FORBIDDEN).build();
      else {
        log.info("Deleting deck: {}", deckId);

        return dr.delete(deck).flatMap(
          c -> ServerResponse.noContent().build()
        );
      }  
    });
    //.switchIfEmpty(ServerResponse.notFound().build());

    return response;

  }
}
