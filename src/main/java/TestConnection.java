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
//      statement.execute("create table Users (id int, username varchar(50));");
//      statement.execute("Insert into users values(1, 'asdf');");
      statement.execute("drop table Users");
    } catch (Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException("failed", e);
    }
  }
}