package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "permissions")
  private String permissions;

  public static Role getByIndex(int index) {
    throw new UnsupportedOperationException("Use RoleRepository instead");
  }

  @Override
  public String toString() {
    return name;
  }

  public static final String OWNER = "Owner";
  public static final String ADMIN = "Admin";
  public static final String PARTICIPANT = "Participant";
}
