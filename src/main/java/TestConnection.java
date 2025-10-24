import java.sql.Connection;

public class TestConnection {
  public static void main(String[] args) {
    try {
      DataRepository repository = new DataRepository();
      testConnection(repository);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testConnection(DataRepository repository) {
    try (Connection connection = repository.getDataSource().getConnection()) {
      var statement = connection.createStatement();
      statement.execute("drop table if exists Users");
    } catch (Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException("test failed", e);
    }
  }
}