package com.coactivity.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.coactivity.persistence.entity.CategoryEntity;
import com.coactivity.persistence.repository.BanJpaRepository;
import com.coactivity.persistence.repository.BulletinBoardJpaRepository;
import com.coactivity.persistence.repository.CategoryLookupRepository;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.persistence.repository.RoleLookupRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.persistence.repository.RoomMemberJpaRepository;
import com.coactivity.persistence.repository.RoomsRequestJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.service.exception.ValidationException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("RoomRepository category lookup tests")
class RoomRepositoryImplTest {

  private CategoryLookupRepository categoryLookupRepository;
  private RoomRepositoryImpl roomRepository;

  @BeforeEach
  void setUp() {
    categoryLookupRepository = Mockito.mock(CategoryLookupRepository.class);

    roomRepository = new RoomRepositoryImpl(
        Mockito.mock(RoomJpaRepository.class),
        Mockito.mock(RoomMemberJpaRepository.class),
        Mockito.mock(BanJpaRepository.class),
        categoryLookupRepository,
        Mockito.mock(RoleLookupRepository.class),
        Mockito.mock(UserJpaRepository.class),
        Mockito.mock(RoomsRequestJpaRepository.class),
        Mockito.mock(BulletinBoardJpaRepository.class),
        Mockito.mock(PictureJpaRepository.class));
  }

  @Test
  @DisplayName("getCategoryIdByName should return category id when lookup exists")
  void getCategoryIdByNameReturnsId() {
    CategoryEntity entity = new CategoryEntity();
    entity.setId(7);
    entity.setName("Sport");
    when(categoryLookupRepository.findByNameIgnoreCase("Sport")).thenReturn(Optional.of(entity));

    assertEquals(7, roomRepository.getCategoryIdByName("SPORT"));
  }

  @Test
  @DisplayName("getCategoryIdByName should throw validation error when category does not exist")
  void getCategoryIdByNameThrowsValidationWhenCategoryMissing() {
    when(categoryLookupRepository.findByNameIgnoreCase("UnknownCategory"))
        .thenReturn(Optional.empty());

    ValidationException exception = assertThrows(ValidationException.class,
        () -> roomRepository.getCategoryIdByName("UnknownCategory"));

    assertEquals("Category not found: UnknownCategory", exception.getMessage());
  }
}
