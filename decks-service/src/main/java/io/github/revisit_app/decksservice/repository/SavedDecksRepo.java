package io.github.revisit_app.decksservice.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.revisit_app.decksservice.entity.SavedDecks;

public interface SavedDecksRepo extends ReactiveCrudRepository<SavedDecks, Long>{
  
}
