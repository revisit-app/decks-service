package io.github.revisit_app.decksservice.handler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.revisit_app.decksservice.entity.Deck;
import io.github.revisit_app.decksservice.entity.UserUDT;
import io.github.revisit_app.decksservice.exception.UserNotFoundException;
import io.github.revisit_app.decksservice.repository.DeckRepo;
import io.github.revisit_app.decksservice.util.DeckData;
import io.github.revisit_app.decksservice.util.NewDeck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckHandler {

  private final DeckRepo dr;
  private final WebClient.Builder wcb;
  private final String userRegistrationServiceUrl = "localhost:8086/";
  private final String userProfileServiceUrl = "localhost:8085/";

  public Mono<ServerResponse> handleGetDeck(ServerRequest request) {
    var deckId = request.pathVariable("id");
    log.info("Getting deck: {}", deckId);

    return dr.findById(deckId)
        .flatMap(deck -> {
          var data = new DeckData(deck);

          return ServerResponse.ok().bodyValue(data);
        })
        .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> handleCreateDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank()) {
      log.error("userId header is missing");

      return ServerResponse.badRequest().build();
    }
    Long userId;
    try {
      userId = Long.valueOf(request.headers().header("userId").get(0));
    } catch (NumberFormatException nfe) {
      log.error("Could not parse userId to Long");

      return ServerResponse.badRequest().build();
    }

    return request.bodyToMono(NewDeck.class)
        .flatMap(cd -> {
          return wcb.build()
              .get()
              .uri(userRegistrationServiceUrl + userId)
              .retrieve()
              .onStatus(HttpStatus::is4xxClientError, res -> Mono.error(new UserNotFoundException()))
              .bodyToMono(UserUDT.class)
              .flatMap(author -> {
                Deck deck = new Deck(
                    UUID.randomUUID().toString(),
                    author,
                    cd.getTitle(),
                    cd.getDesc(),
                    Instant.now(),
                    Instant.now(),
                    Long.valueOf(0),
                    Long.valueOf(0),
                    new ArrayList<>(),
                    new ArrayList<>());

                return wcb.build()
                    .put()
                    .uri(userProfileServiceUrl + userId + "/decks")
                    .header("deckId", deck.getId())
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(res -> {
                      log.info("Creating deck: {}", deck.getId());

                      return dr.save(deck)
                          .flatMap(sd -> ServerResponse.created(URI.create(request.uri() + deck.getId())).build());
                    })
                    .onErrorResume(error -> {
                      log.error("Could not add deck: {} to user: {} 's profile", deck.getId(), userId);

                      return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
              })
              .onErrorResume(error -> {
                if (error instanceof UserNotFoundException) {
                  log.error("User: {} does not exist", userId);

                  return ServerResponse.notFound().build();
                } else {
                  log.error("Something went wrong. Could not verify user: {}", userId);
                  return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
              });
        });
  }

  public Mono<ServerResponse> handleUpdateDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank()) {
      log.error("userId header is missing");

      return ServerResponse.badRequest().build();
    }
    Long userId;
    try {
      userId = Long.valueOf(request.headers().header("userId").get(0));
    } catch (NumberFormatException nfe) {
      log.error("Could not parse userId to Long");

      return ServerResponse.badRequest().build();
    }
    var deckId = request.pathVariable("id");

    return request.bodyToMono(NewDeck.class)
        .flatMap(data -> dr.findById(deckId)
            .flatMap(od -> {
              if (od.getAuthor().getId() != userId) {
                log.error("User: {} is not the author of deck: {}", userId, deckId);

                return ServerResponse.status(HttpStatus.FORBIDDEN).build();
              } else {
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
  }

  public Mono<ServerResponse> handleDeleteDeck(ServerRequest request) {
    if (request.headers().header("userId").isEmpty() || request.headers().header("userId").get(0).isBlank()) {
      log.error("userId header is missing");

      return ServerResponse.badRequest().build();
    }
    Long userId;
    try {
      userId = Long.valueOf(request.headers().header("userId").get(0));
    } catch (NumberFormatException nfe) {
      log.error("Could not parse userId to Long");

      return ServerResponse.badRequest().build();
    }
    var deckId = request.pathVariable("id");

    return dr.findById(deckId)
        .flatMap(deck -> {
          if (deck.getAuthor().getId() != userId) {
            log.error("User: {} is not the author of deck: {}", userId, deckId);

            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
          } else {

            return wcb.build()
                .delete()
                .uri(userProfileServiceUrl + userId + "/decks")
                .header("deckId", deckId)
                .retrieve()
                .toBodilessEntity()
                .flatMap(res -> {
                  if (res.getStatusCode() == HttpStatus.OK) {
                    log.info("Deleting deck: {}", deckId);

                    return dr.delete(deck).flatMap(
                        d -> ServerResponse.noContent().build());
                  } else {
                    log.info("Deleting deck: {}", deckId);

                    return dr.delete(deck).flatMap(
                        d -> ServerResponse.noContent().build());
                  }
                })
                .onErrorResume(error -> {
                  log.error("Could not delete card from user: {} 's profile", userId);
                  return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
          }
        });
  }

  public Mono<ServerResponse> handleAddCard(ServerRequest request) {
    if (request.headers().header("cardId").isEmpty() || request.headers().header("cardId").get(0).isBlank()) {
      log.error("cardId header is missing");

      return ServerResponse.badRequest().build();
    }
    var cardId = request.headers().header("cardId").get(0);
    var deckId = request.pathVariable("id");

    return dr.findById(deckId)
        .flatMap(deck -> {
          var cardIds = deck.getCards();
          var numCards = deck.getNumCards();
          if (cardIds == null)
            cardIds = new ArrayList<>();
          if (!cardIds.contains(cardId)) {
            cardIds.add(cardId);
            numCards += 1;
            deck.setNumCards(numCards);
            deck.setCards(cardIds);
            log.info("Adding card: {} to deck: {}", cardId, deckId);

            return dr.save(deck).flatMap(ud -> ServerResponse.created(URI.create(request.uri() + deckId)).build());
          } else {
            log.info("Card: {} is already in deck: {}", cardId, deckId);

            return ServerResponse.noContent().build();
          }
        })
        .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> handleRemoveCard(ServerRequest request) {
    if (request.headers().header("cardId").isEmpty() || request.headers().header("cardId").get(0).isBlank()) {
      log.error("cardId header is missing");

      return ServerResponse.badRequest().build();
    }
    var cardId = request.headers().header("cardId").get(0);
    var deckId = request.pathVariable("id");

    return dr.findById(deckId)
        .flatMap(deck -> {
          var cardIds = deck.getCards();
          var numCards = deck.getNumCards();
          if (cardIds == null)
            cardIds = new ArrayList<>();
          if (cardIds.contains(cardId) && numCards > 0) {
            cardIds.remove(cardId);
            numCards -= 1;
            deck.setNumCards(numCards);
            deck.setCards(cardIds);
            log.info("Removing card: {} from deck: {}", cardId, deckId);

            return dr.save(deck).flatMap(ud -> ServerResponse.noContent().build());
          } else {
            log.info("Deck: {} does not contain card: {}", cardId, deckId);

            return ServerResponse.noContent().build();
          }
        })
        .switchIfEmpty(ServerResponse.notFound().build());
  }
}
