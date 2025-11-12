package com.coactivity.repository.impl;

import com.coactivity.DataRepository;
import com.coactivity.domain.Picture;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.domain.Category;
import com.coactivity.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PictureRepositoryImplTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private PictureRepositoryImpl pictureRepository;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    private User createMockUser(int id) {
        return new User(
                id,
                "user" + id,
                "User " + id,
                "password123",
                Instant.now().minusSeconds(86400 * 365 * 25),
                "City " + id,
                "Country " + id,
                "Description of user " + id,
                1,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private Room createMockRoom(int id) {
        Map<User, Role> users = new HashMap<>();
        users.put(createMockUser(1), Role.Owner);

        List<User> bans = new ArrayList<>();

        return new Room(
                id,
                true,
                true,
                "chat-link-" + id,
                Category.Sport,
                "Room " + id,
                "Description of room " + id,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                18,
                0,
                10,
                users,
                bans
        );
    }

    @Test
    void createPicture_Success() throws SQLException {
        // Arrange
        int roomId = 1;
        int generatedPictureId = 10;
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("picture_id")).thenReturn(generatedPictureId);

            // Act
            Picture result = pictureRepository.createPicture(roomId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedPictureId, result.getPhotoId()); // Исправлено на getPhotoId()
            assertEquals(mockRoom, result.getRoom());

            verify(preparedStatement).setInt(1, roomId);
        }
    }

    @Test
    void createPicture_NoResultSet_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    pictureRepository.createPicture(roomId)
            );
        }
    }

    @Test
    void createPicture_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    pictureRepository.createPicture(roomId)
            );
        }
    }

    @Test
    void createPicture_WithNonExistentRoom() throws SQLException {
        // Arrange
        int roomId = 999;
        int generatedPictureId = 10;

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(null))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("picture_id")).thenReturn(generatedPictureId);

            // Act
            Picture result = pictureRepository.createPicture(roomId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedPictureId, result.getPhotoId());
            assertNull(result.getRoom());
        }
    }

    @Test
    void getRoomPictures_Success() throws SQLException {
        // Arrange
        int roomId = 1;
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            when(resultSet.getInt("picture_id")).thenReturn(1, 2);
            when(resultSet.getInt("room_id")).thenReturn(roomId, roomId);

            // Act
            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);

            // Assert
            assertNotNull(pictures);
            assertEquals(2, pictures.size());

            Picture firstPicture = pictures.get(0);
            assertEquals(1, firstPicture.getPhotoId());
            assertEquals(mockRoom, firstPicture.getRoom());

            Picture secondPicture = pictures.get(1);
            assertEquals(2, secondPicture.getPhotoId());
            assertEquals(mockRoom, secondPicture.getRoom());

            verify(preparedStatement).setInt(1, roomId);
        }
    }

    @Test
    void getRoomPictures_EmptyResult() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            // Act
            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);

            // Assert
            assertNotNull(pictures);
            assertTrue(pictures.isEmpty());
        }
    }

    @Test
    void getRoomPictures_WithMultipleRooms() throws SQLException {
        // Arrange
        int roomId1 = 1;
        int roomId2 = 2;
        Room mockRoom1 = createMockRoom(roomId1);
        Room mockRoom2 = createMockRoom(roomId2);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> {
                                 when(mock.getRoomById(roomId1)).thenReturn(mockRoom1);
                                 when(mock.getRoomById(roomId2)).thenReturn(mockRoom2);
                             })) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);

            when(resultSet.getInt("picture_id")).thenReturn(1, 2);
            when(resultSet.getInt("room_id")).thenReturn(roomId1, roomId2);

            // Act
            List<Picture> pictures = pictureRepository.getRoomPictures(roomId1);

            // Assert
            assertEquals(2, pictures.size());
            assertEquals(mockRoom1, pictures.get(0).getRoom());
            assertEquals(mockRoom2, pictures.get(1).getRoom());
        }
    }

    @Test
    void getRoomPictures_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int roomId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    pictureRepository.getRoomPictures(roomId)
            );
        }
    }

    @Test
    void deletePicture_Success() throws SQLException {
        // Arrange
        int photoId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act & Assert
            assertDoesNotThrow(() -> pictureRepository.deletePicture(photoId));

            // Assert
            verify(preparedStatement).setInt(1, photoId);
            verify(preparedStatement).executeUpdate();
        }
    }

    @Test
    void deletePicture_NoRowsAffected_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int photoId = 999;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(0);

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    pictureRepository.deletePicture(photoId)
            );
        }
    }

    @Test
    void deletePicture_SQLException_ThrowsRuntimeException() throws SQLException {
        // Arrange
        int photoId = 1;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    pictureRepository.deletePicture(photoId)
            );
        }
    }

    @Test
    void mapResultSetToPicture_Success() throws SQLException {
        // Arrange
        int pictureId = 5;
        int roomId = 1;
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(resultSet.getInt("picture_id")).thenReturn(pictureId);
            when(resultSet.getInt("room_id")).thenReturn(roomId);

            // Act (через вызов getRoomPictures)
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);

            // Assert
            assertEquals(1, pictures.size());
            Picture picture = pictures.get(0);
            assertEquals(pictureId, picture.getPhotoId());
            assertEquals(mockRoom, picture.getRoom());
        }
    }

    @Test
    void mapResultSetToPicture_WithNullRoom() throws SQLException {
        // Arrange
        int pictureId = 5;
        int roomId = 999;

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(null))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(resultSet.getInt("picture_id")).thenReturn(pictureId);
            when(resultSet.getInt("room_id")).thenReturn(roomId);

            // Act (через вызов getRoomPictures)
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);

            // Assert
            assertEquals(1, pictures.size());
            Picture picture = pictures.get(0);
            assertEquals(pictureId, picture.getPhotoId());
            assertNull(picture.getRoom());
        }
    }

    @Test
    void createPicture_ColumnNameMismatch_Fixed() throws SQLException {
        // Arrange
        int roomId = 1;
        int generatedPictureId = 10;
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // Исправлено: должно быть "picture_id" вместо "pictureId"
            when(resultSet.getInt("picture_id")).thenReturn(generatedPictureId);

            // Act
            Picture result = pictureRepository.createPicture(roomId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedPictureId, result.getPhotoId());
        }
    }

    @Test
    void getRoomPictures_WithLargeNumberOfPictures() throws SQLException {
        // Arrange
        int roomId = 1;
        int pictureCount = 50;
        Room mockRoom = createMockRoom(roomId);

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(mockRoom))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            // Симулируем 50 записей
            when(resultSet.next()).thenAnswer(invocation -> {
                int callCount = invocation.getArgument(0, Integer.class);
                return callCount < pictureCount;
            });

            when(resultSet.getInt("picture_id")).thenAnswer(invocation ->
                    invocation.getArgument(0, Integer.class) + 1);
            when(resultSet.getInt("room_id")).thenReturn(roomId);

            // Act
            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);

            // Assert
            assertEquals(pictureCount, pictures.size());

            // Проверяем, что все картинки имеют правильные ID и комнату
            for (int i = 0; i < pictureCount; i++) {
                Picture picture = pictures.get(i);
                assertEquals(i + 1, picture.getPhotoId());
                assertEquals(mockRoom, picture.getRoom());
            }
        }
    }

    @Test
    void deletePicture_WithZeroPhotoId() throws SQLException {
        // Arrange
        int photoId = 0;

        try (MockedConstruction<RoomRepositoryImpl> ignored =
                     mockConstruction(RoomRepositoryImpl.class)) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Act & Assert
            assertDoesNotThrow(() -> pictureRepository.deletePicture(photoId));

            verify(preparedStatement).setInt(1, photoId);
        }
    }

    @Test
    void createPicture_WithNegativeRoomId() throws SQLException {
        // Arrange
        int roomId = -1;
        int generatedPictureId = 10;

        try (MockedConstruction<RoomRepositoryImpl> roomRepoMock =
                     mockConstruction(RoomRepositoryImpl.class,
                             (mock, context) -> when(mock.getRoomById(roomId)).thenReturn(null))) {

            pictureRepository = new PictureRepositoryImpl(dataRepository);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("picture_id")).thenReturn(generatedPictureId);

            // Act
            Picture result = pictureRepository.createPicture(roomId);

            // Assert
            assertNotNull(result);
            assertEquals(generatedPictureId, result.getPhotoId());
            assertNull(result.getRoom());
        }
    }

    @Test
    void pictureConstructor_ValidParameters() {
        // Arrange
        Room mockRoom = createMockRoom(1);
        int photoId = 5;

        // Act
        Picture picture = new Picture(mockRoom, photoId);

        // Assert
        assertNotNull(picture);
        assertEquals(mockRoom, picture.getRoom());
        assertEquals(photoId, picture.getPhotoId());
    }

    @Test
    void pictureConstructor_NullRoom() {
        // Arrange
        int photoId = 5;

        // Act
        Picture picture = new Picture(null, photoId);

        // Assert
        assertNotNull(picture);
        assertNull(picture.getRoom());
        assertEquals(photoId, picture.getPhotoId());
    }

    @Test
    void pictureGettersAndSetters() {
        // Arrange
        Room initialRoom = createMockRoom(1);
        int initialPhotoId = 5;
        Picture picture = new Picture(initialRoom, initialPhotoId);

        // Act
        Room newRoom = createMockRoom(2);
        int newPhotoId = 10;
        picture.setRoom(newRoom);
        picture.setPhotoId(newPhotoId);

        // Assert
        assertEquals(newRoom, picture.getRoom());
        assertEquals(newPhotoId, picture.getPhotoId());
    }
}