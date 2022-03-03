package io.github.revisit_app.decksservice.util;

import java.time.Instant;
import java.util.List;

import io.github.revisit_app.decksservice.entity.Deck;
import io.github.revisit_app.decksservice.entity.UserUDT;
import lombok.Data;

@Data
public class DeckData {

  private String id;

  private UserUDT author;

  private String title;

  private String desc;

  private Instant dateCreated;

  private Instant dateUpdated;

  private Long numCards;

  private Long numSaves;

  private List<String> cards;

  public DeckData(Deck deck) {
    this.id = deck.getId();
    this.author = deck.getAuthor();
    this.title = deck.getTitle();
    this.desc = deck.getDesc();
    this.dateCreated = deck.getDateCreated();
    this.dateUpdated = deck.getDateUpdated();
    this.numCards = deck.getNumCards();
    this.numSaves = deck.getNumSaves();
    this.cards = deck.getCards();
  }
}
