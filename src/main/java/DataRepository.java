import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;

public class DataRepository {
  private DataSource dataSource;

  public DataRepository() {
    setupDataSourceFromConfig();
  }

  private void setupDataSourceFromConfig() {
    PGSimpleDataSource ds = new PGSimpleDataSource();

    try {
      InputStream inputStream = new ClassPathResource("config.json").getInputStream();
      JsonNode jsonNode = new ObjectMapper().readTree(inputStream);
      ds.setServerName(jsonNode.get("host").asText());
      ds.setPortNumber(jsonNode.get("port").asInt());
      ds.setDatabaseName(jsonNode.get("database").asText());
      ds.setUser(jsonNode.get("username").asText());
      ds.setPassword(jsonNode.get("password").asText());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.dataSource = ds;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}