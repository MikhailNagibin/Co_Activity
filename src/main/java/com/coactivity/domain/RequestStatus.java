package com.coactivity.domain;

public enum RequestStatus {
  CONSIDERATION,
  ACCEPTED,
  REFUSED,
  REFUSED_WITH_BAN;

  // Метод для получения значения для БД
  public String toDatabaseValue() {
    switch (this) {
      case CONSIDERATION: return "Consideration";
      case ACCEPTED: return "Accepted";
      case REFUSED: return "Refused";
      case REFUSED_WITH_BAN: return "RefusedWithBan";
      default: throw new IllegalStateException("Unknown status: " + this);
    }
  }

  // Метод для преобразования из БД
  public static RequestStatus fromDatabase(String dbValue) {
    if (dbValue == null) return null;

    String trimmed = dbValue.trim();
    switch (trimmed) {
      case "Consideration": return CONSIDERATION;
      case "Accepted": return ACCEPTED;
      case "Refused": return REFUSED;
      case "RefusedWithBan": return REFUSED_WITH_BAN;
      default:
        // Регистронезависимое сравнение
        String upper = trimmed.toUpperCase();
        if (upper.equals("CONSIDERATION")) return CONSIDERATION;
        if (upper.equals("ACCEPTED")) return ACCEPTED;
        if (upper.equals("REFUSED")) return REFUSED;
        if (upper.equals("REFUSEDWITHBAN")) return REFUSED_WITH_BAN;
        throw new IllegalArgumentException("Unknown status in DB: '" + dbValue + "'");
    }
  }
}