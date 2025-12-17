package com.coactivity;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;

public class TestConnection {

  public static void main(String[] args) {
    try {
      DataRepository repository = new DataRepository();
      executeSqlScript(repository, "src//main/resources/sql/init_tables.sql");
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  private static void executeSqlScript(DataRepository repository, String scriptPath) {
    try (Connection connection = repository.getDataSource().getConnection()) {
      // Читаем SQL файл
      String sqlScript = new String(Files.readAllBytes(Paths.get(scriptPath)));

      // Разделяем на отдельные команды
      String[] commands = sqlScript.split(";");

      var statement = connection.createStatement();
      for (String command : commands) {
        if (!command.trim().isEmpty()) {
          System.out.println("Executing: " + command.trim());
          statement.execute(command);
        }
      }
      System.out.println("SQL script executed successfully!");

    } catch (Exception e) {
      System.err.println("Error executing SQL script: " + e.getMessage());
      throw new RuntimeException("Failed to execute SQL script", e);
    }
  }
}