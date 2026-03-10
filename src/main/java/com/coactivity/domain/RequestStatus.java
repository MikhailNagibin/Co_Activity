package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    // Для обратной совместимости с существующим кодом
    public String toDatabaseValue() {
        return name;
    }

    public static RequestStatus fromDatabase(String dbValue) {
        throw new UnsupportedOperationException("Use RequestStatusRepository instead");
    }

    // Предопределенные константы
    public static final String CONSIDERATION = "Consideration";
    public static final String ACCEPTED = "Accepted";
    public static final String REFUSED = "Refused";
    public static final String REFUSED_WITH_BAN = "RefusedWithBan";
}
