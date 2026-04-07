package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.domain.Category;
import com.coactivity.domain.Picture;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.repository.PictureRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.support.TestImageFactory;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.storage.FileStorage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("RoomImageService tests")
class RoomImageServiceTest {

  private RoomRepository roomRepository;
  private PictureRepository pictureRepository;
  private FileStorage fileStorage;
  private RoomImageService roomImageService;

  @BeforeEach
  void setUp() {
    roomRepository = Mockito.mock(RoomRepository.class);
    pictureRepository = Mockito.mock(PictureRepository.class);
    fileStorage = Mockito.mock(FileStorage.class);
    roomImageService = new RoomImageService(roomRepository, pictureRepository, fileStorage, 5_242_880);

    when(roomRepository.getRoomById(42)).thenReturn(room(42));
    when(roomRepository.isUserInMembers(42, 7)).thenReturn(true);
    when(roomRepository.getUserRoleByRoomId(42, 7)).thenReturn(Role.OWNER);
  }

  @Test
  void uploadRoomImagesAppendsImagesInOrder() {
    MockMultipartFile firstFile = new MockMultipartFile(
        "files",
        "cover.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.png());
    MockMultipartFile secondFile = new MockMultipartFile(
        "files",
        "gallery.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        TestImageFactory.jpeg());

    when(pictureRepository.countRoomPictures(42)).thenReturn(2L);
    when(pictureRepository.getRoomPictures(42)).thenReturn(List.of(
        picture(42, 1, 1, "room-images/1.png"),
        picture(42, 2, 2, "room-images/2.png"),
        picture(42, 3, 3, "room-images/3.png"),
        picture(42, 4, 4, "room-images/4.jpg")));

    var response = roomImageService.uploadRoomImages(7, 42, new MockMultipartFile[]{firstFile, secondFile});

    verify(pictureRepository).createPicture(eq(42), Mockito.anyString(), eq("cover.png"),
        eq(MediaType.IMAGE_PNG_VALUE), eq((long) firstFile.getSize()), eq(3));
    verify(pictureRepository).createPicture(eq(42), Mockito.anyString(), eq("gallery.jpg"),
        eq(MediaType.IMAGE_JPEG_VALUE), eq((long) secondFile.getSize()), eq(4));
    assertEquals(List.of(1, 2, 3, 4), response.stream().map(image -> image.getOrder()).toList());
  }

  @Test
  void uploadRoomImagesDeletesStoredFilesWhenPersistenceFailsMidway() {
    MockMultipartFile firstFile = new MockMultipartFile(
        "files",
        "first.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.png());
    MockMultipartFile secondFile = new MockMultipartFile(
        "files",
        "second.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.png());

    when(pictureRepository.countRoomPictures(42)).thenReturn(0L);
    when(pictureRepository.createPicture(eq(42), Mockito.anyString(), eq("first.png"),
        eq(MediaType.IMAGE_PNG_VALUE), eq((long) firstFile.getSize()), eq(1)))
        .thenReturn(picture(42, 1, 1, "room-images/created-1.png"));
    when(pictureRepository.createPicture(eq(42), Mockito.anyString(), eq("second.png"),
        eq(MediaType.IMAGE_PNG_VALUE), eq((long) secondFile.getSize()), eq(2)))
        .thenThrow(new RuntimeException("db failure"));

    assertThrows(RuntimeException.class,
        () -> roomImageService.uploadRoomImages(7, 42, new MockMultipartFile[]{firstFile, secondFile}));

    ArgumentCaptor<String> saveKeys = ArgumentCaptor.forClass(String.class);
    verify(fileStorage, times(2)).save(saveKeys.capture(), Mockito.any());
    List<String> storedKeys = saveKeys.getAllValues();
    verify(fileStorage).delete(storedKeys.get(0));
    verify(fileStorage).delete(storedKeys.get(1));
    verify(pictureRepository, never()).getRoomPictures(anyInt());
  }

  @Test
  void deleteRoomImageReordersRemainingImages() {
    when(pictureRepository.getRoomPicture(42, 2)).thenReturn(picture(42, 2, 2, "room-images/2.png"));
    when(pictureRepository.getRoomPictures(42))
        .thenReturn(List.of(
            picture(42, 1, 1, "room-images/1.png"),
            picture(42, 3, 3, "room-images/3.png")))
        .thenReturn(List.of(
            picture(42, 1, 1, "room-images/1.png"),
            picture(42, 3, 2, "room-images/3.png")));

    var response = roomImageService.deleteRoomImage(7, 42, 2);

    verify(pictureRepository).deletePicture(2);
    verify(pictureRepository).updatePictureSortOrder(3, 2);
    verify(fileStorage).delete("room-images/2.png");
    assertEquals(List.of(1, 2), response.stream().map(image -> image.getOrder()).toList());
  }

  @Test
  void uploadRoomImagesRejectsRequestThatWouldExceedRoomLimit() {
    MockMultipartFile imageFile = new MockMultipartFile(
        "files",
        "cover.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.png());

    when(pictureRepository.countRoomPictures(42)).thenReturn(5L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomImageService.uploadRoomImages(7, 42, new MockMultipartFile[]{imageFile}));

    assertEquals("Room cannot have more than 5 images", exception.getMessage());
    verify(fileStorage, never()).save(Mockito.anyString(), Mockito.any());
  }

  @Test
  void uploadRoomImagesRejectsSpoofedPayloadWithImageMimeType() {
    MockMultipartFile imageFile = new MockMultipartFile(
        "files",
        "cover.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.invalidImagePayload());

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomImageService.uploadRoomImages(7, 42, new MockMultipartFile[]{imageFile}));

    assertEquals("Room image content does not match declared image type", exception.getMessage());
    verify(fileStorage, never()).save(Mockito.anyString(), Mockito.any());
  }

  private Room room(Integer roomId) {
    return new Room(
        roomId,
        true,
        true,
        null,
        Category.SPORT,
        "Morning Run",
        "Test room",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        18,
        null,
        10,
        java.util.Map.of(),
        List.of());
  }

  private Picture picture(Integer roomId, Integer imageId, Integer order, String storageKey) {
    return new Picture(
        room(roomId),
        imageId,
        storageKey,
        "image.png",
        MediaType.IMAGE_PNG_VALUE,
        10L,
        order,
        Instant.now());
  }
}
