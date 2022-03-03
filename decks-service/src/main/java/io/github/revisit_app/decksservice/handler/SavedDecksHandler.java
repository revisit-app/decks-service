package io.github.revisit_app.decksservice.handler;

import java.util.ArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.revisit_app.decksservice.repository.DeckRepo;
import io.github.revisit_app.decksservice.repository.SavedDecksRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SavedDecksHandler {

  private final SavedDecksRepo sdr;
  private final DeckRepo dr;

  public Mono<ServerResponse> handleGetSavedDecks(ServerRequest request) {
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
    log.info("Getting saved decks for user: {}", userId);

    return sdr.findById(userId)
        .flatMap(sd -> {
          var savedDecks = sd.getSaved_decks();
          if (savedDecks != null)
            return ServerResponse.ok().bodyValue(savedDecks);
          else
            return ServerResponse.ok().bodyValue(new ArrayList<>());
        })
        .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> handleAddDeck(ServerRequest request) {
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
          log.info("Deck: {} verified", deckId);

          if (deck.getAuthor().getId() == userId) {
            log.info("User is the author of the deck: {}. Deck is already saved.", deckId);

            return ServerResponse.noContent().build();
          } else {
            var savedBy = deck.getSavedBy();
            var numSaves = deck.getNumSaves();

            if (savedBy == null) {
              savedBy = new ArrayList<>();
            }
            if (!savedBy.contains(userId)) {
              savedBy.add(userId);
              deck.setSavedBy(savedBy);
              numSaves += 1;
              deck.setNumSaves(numSaves);
              log.info("User: {} saved card: {}", userId, deckId);

              return dr.save(deck)
                  .flatMap(ud -> {

                    return sdr.findById(userId)
                        .flatMap(sd -> {
                          var savedDecks = sd.getSaved_decks();
                          if (savedDecks == null) {
                            savedDecks = new ArrayList<>();
                          }
                          log.info("Saving deck: {} for user: {}", deckId, userId);
                          savedDecks.add(deckId);
                          sd.setSaved_decks(savedDecks);

                          return sdr.save(sd)
                              .flatMap(ok -> ServerResponse.ok().build());
                        });
                  });
            } else {
              log.info("Deck: {} is already saved by user: {}", deckId, userId);

              return ServerResponse.noContent().build();
            }
          }
        })
        .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> handleRemoveDeck(ServerRequest request) {
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
        .flatMap(
            deck -> {
              log.info("Deck: {} verified", deckId);
              var savedBy = deck.getSavedBy();
              var numSaves = deck.getNumSaves();
              if (savedBy != null && savedBy.contains(userId) && numSaves > 0) {
                savedBy.remove(userId);
                deck.setSavedBy(savedBy);
                numSaves -= 1;
                deck.setNumSaves(numSaves);
                log.info("Removing deck: {} for user: {}", deckId, userId);

                return dr.save(deck)
                    .flatMap(ud -> {

                      return sdr.findById(userId)
                          .flatMap(sd -> {
                            var savedDecks = sd.getSaved_decks();
                            if (savedDecks == null) {
                              savedDecks = new ArrayList<>();
                            }
                            savedDecks.remove(deckId);
                            sd.setSaved_decks(savedDecks);

                            return sdr.save(sd)
                                .flatMap(ok -> ServerResponse.ok().build());
                          });
                    });
              } else {
                log.error("Deck: {} is not saved by user: {}", deckId, userId);

                return ServerResponse.badRequest().build();
              }
            });
  }
}
