package com.coactivity.domain;


public enum Role {
  OWNER("owner"),
  ADMIN("admin"),
  PARTICIPANT("participant");

  private final String roleName;

  public static Role getByIndex(int index) {
    Role[] roles = values();
    if (index >= 0 && index < roles.length) {
      return roles[index];
    }
    throw new IllegalArgumentException("Invalid role index: " + index);
  }

  Role(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public String toString() {
    return roleName;
  }
}
