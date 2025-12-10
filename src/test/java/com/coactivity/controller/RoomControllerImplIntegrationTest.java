//package com.coactivity.controller;
//
//import com.coactivity.DataRepository;
//import com.coactivity.controller.dto.request.RoomCreationRequest;
//import com.coactivity.controller.dto.response.ApiResponse;
//import com.coactivity.controller.dto.response.BulletinBoardResponse;
//import com.coactivity.controller.dto.response.RoomCreationResponse;
//import com.coactivity.controller.dto.response.RoomDetailedResponse;
//import com.coactivity.controller.dto.response.RoomSummaryResponse;
//import com.coactivity.controller.impl.RoomControllerImpl;
//import com.coactivity.repository.impl.*;
//import com.coactivity.service.*;
//import com.coactivity.service.dto.TokenPayload;
//import java.nio.charset.StandardCharsets;
//import java.sql.Connection;
//import java.sql.Statement;
//import java.time.Instant;
//import java.util.List;
//import javax.sql.DataSource;
//import org.junit.jupiter.api.*;
//import org.springframework.core.io.ClassPathResource;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@Testcontainers
//public class RoomControllerImplIntegrationTest {
//
//  @Container
//  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
//
//  static DataRepository dataRepository;
//  static UserRepositoryImpl userRepository;
//  static RoomRepositoryImpl roomRepository;
//  static RoomsRequestRepositoryImpl roomsRequestRepository;
//  static PictureRepositoryImpl pictureRepository;
//  static BulletinBoardRepositoryImpl bulletinBoardRepository;
//  static RoomService roomService;
//  static UserWithRoomService userWithRoomService;
//  static BulletinBoardService bulletinBoardService;
//  static TokenService tokenService;
//  static RoomControllerImpl controller;
//
//  @BeforeAll
//  static void setup() throws Exception {
//    postgres.start();
//    runInitSql();
//    seedLookupsAndUsers();
//
//    dataRepository = buildDataRepositoryFromContainer();
//
//    userRepository = new UserRepositoryImpl(dataRepository, null);
//    roomRepository = new RoomRepositoryImpl(dataRepository, userRepository);
//    userRepository = new UserRepositoryImpl(dataRepository, roomRepository);
//
//    roomsRequestRepository = new RoomsRequestRepositoryImpl(dataRepository, roomRepository, userRepository);
//    pictureRepository = new PictureRepositoryImpl(dataRepository, roomRepository);
//    bulletinBoardRepository = new BulletinBoardRepositoryImpl(dataRepository, userRepository, roomRepository);
//
//    tokenService = new TokenService() {
//      @Override public boolean isTokenActive(String token) { return token != null; }
//      @Override public TokenPayload decodeToken(String token) { return new TokenPayload(Integer.parseInt(token), Instant.now()); }
//    };
//
//    roomService = new RoomService(roomRepository, tokenService, pictureRepository, bulletinBoardRepository);
//    userWithRoomService = new UserWithRoomService(userRepository, roomRepository, roomsRequestRepository, tokenService, pictureRepository, bulletinBoardRepository);
//    bulletinBoardService = new BulletinBoardService(bulletinBoardRepository, roomRepository, userRepository);
//
//    controller = new RoomControllerImpl(roomService, tokenService, userWithRoomService, bulletinBoardService);
//  }
//
//  @AfterAll
//  static void teardown() { postgres.stop(); }
//
//  private static DataRepository buildDataRepositoryFromContainer() {
//    return new DataRepository() {
//      final DataSource ds = new org.postgresql.ds.PGSimpleDataSource() {{
//        setServerName(postgres.getHost());
//        setPortNumber(postgres.getFirstMappedPort());
//        setDatabaseName(postgres.getDatabaseName());
//        setUser(postgres.getUsername());
//        setPassword(postgres.getPassword());
//      }};
//      @Override public DataSource getDataSource() { return ds; }
//    };
//  }
//
//  private static void runInitSql() throws Exception {
//    var res = new ClassPathResource("sql/init_tables.sql");
//    String sql = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//    try (Connection c = buildDataRepositoryFromContainer().getDataSource().getConnection(); Statement st = c.createStatement()) {
//      for (String s : sql.split(";")) { s = s.trim(); if (!s.isEmpty()) st.execute(s); }
//    }
//  }
//
//  private static void seedLookupsAndUsers() throws Exception {
//    try (Connection c = buildDataRepositoryFromContainer().getDataSource().getConnection(); Statement st = c.createStatement()) {
//      st.execute("INSERT INTO Roles(role) VALUES ('OWNER'),('ADMIN'),('PARTICIPANT') ON CONFLICT DO NOTHING");
//      st.execute("INSERT INTO Categories(name) VALUES ('Sport') ON CONFLICT DO NOTHING");
//      st.execute("INSERT INTO Users (login, username, password, birthday, country, city, description, avatar_id) VALUES (" +
//          "'u1','user1','p', now(), 'ctry','city','desc',1)," +
//          "('u2','user2','p', now(), 'ctry','city','desc',2)");
//      st.execute("INSERT INTO RequestStatuses(status_info) VALUES ('CONSIDERATION'),('ACCEPTED'),('REFUSED'),('REFUSEDWITHBAN') ON CONFLICT DO NOTHING");
//    }
//  }
//
//  @Test
//  @DisplayName("createRoom, getRooms, getRoomById")
//  void roomCreateListGet() {
//    RoomCreationRequest req = new RoomCreationRequest();
//    req.setName("Room A");
//    req.setCategoryId(1);
//    req.setIsPublic(true);
//    req.setMaximumNumberOfPeople(10);
//
//    ApiResponse<RoomCreationResponse> created = controller.createRoom("1", req);
//    assertTrue(created.isSuccess());
//    Integer roomId = created.getData().getRoomId();
//
//    ApiResponse<List<RoomSummaryResponse>> list = controller.getRooms(null, null, null);
//    assertTrue(list.isSuccess());
//    assertTrue(list.getData().stream().anyMatch(r -> r.getId().equals(roomId)));
//
//    ApiResponse<RoomDetailedResponse> byId = controller.getRoomById(roomId, "1");
//    assertTrue(byId.isSuccess());
//    assertEquals(roomId, byId.getData().getId());
//  }
//
//  @Test
//  @DisplayName("updateBulletinBoard")
//  void updateBulletinBoard_success() {
//    RoomCreationRequest req = new RoomCreationRequest();
//    req.setName("Room B");
//    req.setCategoryId(1);
//    req.setIsPublic(true);
//    req.setMaximumNumberOfPeople(10);
//    Integer roomId = controller.createRoom("1", req).getData().getRoomId();
//
//    ApiResponse<BulletinBoardResponse> updated = controller.updateBulletinBoard("1", roomId, "Hello");
//    assertTrue(updated.isSuccess());
//    assertEquals("Hello", updated.getData().getContent());
//  }
//
//  @Test
//  @DisplayName("joinRoom public room")
//  void joinRoom_success() {
//    RoomCreationRequest req = new RoomCreationRequest();
//    req.setName("Room C");
//    req.setCategoryId(1);
//    req.setIsPublic(true);
//    req.setMaximumNumberOfPeople(2);
//    Integer roomId = controller.createRoom("1", req).getData().getRoomId();
//
//    ApiResponse<Void> join = controller.joinRoom("2", roomId);
//    assertTrue(join.isSuccess());
//
//    ApiResponse<RoomDetailedResponse> byId = controller.getRoomById(roomId, "2");
//    assertTrue(byId.isSuccess());
//    assertTrue(Boolean.TRUE.equals(byId.getData().getIsCurrentUserParticipant()));
//  }
//}
