import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class TestConnection {

  public static void main(String[] args) {
    try {
      DataRepository repository = new DataRepository();
      testConnection(repository);
    } catch (Exception e) {
      System.err.println("test failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testConnection(DataRepository repository) {
    try (Connection connection = repository.getDataSource().getConnection()) {
      var statement = connection.createStatement();
      statement.execute("drop table if exists Users");
    } catch (Exception e) {
      System.err.println("Connection failed: " + e.getMessage());
      throw new RuntimeException("Database connection test failed", e);
    }
  }
}