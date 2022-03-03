package io.github.revisit_app.decksservice.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("saved_decks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedDecks {
  
  @Id
  private Long id;

  @Column("saved_decks")
  private List<String> saved_decks;
}
