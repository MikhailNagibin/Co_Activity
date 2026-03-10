package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  // Статические методы для обратной совместимости
  public static Category getByIndex(int index) {
    // Этот метод должен быть реализован через репозиторий
    // Оставляем заглушку, которая будет переопределена через Spring
    throw new UnsupportedOperationException("Use CategoryRepository instead");
  }

  public static final String SPORT = "SPORT";
  public static final String MUSIC = "MUSIC";
  public static final String ART = "ART";
  public static final String ENTERTAINMENTS = "ENTERTAINMENTS";
  public static final String BUSINESS = "BUSINESS";
  public static final String EDUCATION = "EDUCATION";
  public static final String ACTIVE_RECREATION = "ACTIVE_RECREATION";
  public static final String PASSIVE_RECREATION = "PASSIVE_RECREATION";
  public static final String IS_A_MASS_EVENT = "IS_A_MASS_EVENT";
  public static final String OTHER = "OTHER";
  public static final String NOT_SPECIFIED = "NOT_SPECIFIED";
}
