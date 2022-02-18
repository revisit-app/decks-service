package io.github.revisit_app.decksservice.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deck {
    
    @Id
    private String id;

    private UserUDT author;

    private String title;

    @Column("description")
    private String desc;

    @Column("date_created")
    private Instant dateCreated;

    @Column("date_updated")
    private Instant dateUpdated;

    @Column("num_cards")
    private int numCards;

    @Column("num_saves")
    private int numSaves;

    @Column("num_likes")
    private int numLikes;

    private List<String> cards;

    @Column("saved_by")
    private List<String> savedBy;
}
