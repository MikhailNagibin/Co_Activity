package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.RoomInvitationEntity;
import com.coactivity.persistence.entity.RoomInvitationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomInvitationJpaRepository
    extends JpaRepository<RoomInvitationEntity, RoomInvitationId> {

  boolean existsByRoom_IdAndInvitedUser_Id(Integer roomId, Integer invitedUserId);

  void deleteByRoom_IdAndInvitedUser_Id(Integer roomId, Integer invitedUserId);

  void deleteAllByRoom_Id(Integer roomId);

  @Modifying
  @Query("""
      delete from RoomInvitationEntity invitation
      where invitation.invitedUser.id = :userId
         or invitation.invitedByUser.id = :userId
      """)
  void deleteAllByUserId(@Param("userId") Integer userId);

  @Modifying
  @Query(value = """
      INSERT INTO room_invitations (room_id, invited_user_id, invited_by_user_id)
      VALUES (:roomId, :invitedUserId, :invitedByUserId)
      ON CONFLICT (room_id, invited_user_id) DO NOTHING
      """, nativeQuery = true)
  int createIfAbsent(@Param("roomId") Integer roomId,
      @Param("invitedUserId") Integer invitedUserId,
      @Param("invitedByUserId") Integer invitedByUserId);
}
