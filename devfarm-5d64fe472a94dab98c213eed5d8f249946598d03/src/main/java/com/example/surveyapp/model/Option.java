package com.example.surveyapp.model;

import jakarta.persistence.*;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity
@Table(name = "options")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "text")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // voteCount alanının veritabanında karşılığı vote_count olarak belirtildi
    // Ayrıca columnDefinition ile varsayılan değer 0 olarak ayarlandı
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0; // Varsayılan değer atandı

    // Getter ve Setter metodları
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Integer getVoteCount() { return voteCount != null ? voteCount : 0; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }

    // toString, equals, hashCode
    @Override
    public String toString() {
        return "Option{id=" + id + ", text='" + text + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return Objects.equals(id, option.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}