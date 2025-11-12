package com.coactivity.domain;


public enum Role {
  OWNER,
  ADMIN,
  PARTICIPANT;

  public static Role getByIndex(int index) {
    Role[] roles = values();
    if (index >= 0 && index < roles.length) {
      return roles[index];
    }
    throw new IllegalArgumentException("Invalid role index: " + index);
  }
}
