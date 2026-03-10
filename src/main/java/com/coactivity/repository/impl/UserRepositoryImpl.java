package com.coactivity.repository.impl;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.*;
import com.coactivity.repository.impl.NotificationRepository;
import com.coactivity.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Transactional
public class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final RoomRepositoryImpl roomRepository;
    private final NotificationRepository notificationRepository;

    public UserRepositoryImpl(@Lazy RoomRepositoryImpl roomRepository,
                              NotificationRepository notificationRepository) {
        this.roomRepository = roomRepository;
        this.notificationRepository = notificationRepository;
    }

    private static String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public User createUser(UserRegistrationRequest request) {
        try {
            User user = new User();
            user.setLogin(request.getLogin());
            user.setPassword(sha256(request.getPassword()));
            user.setDataOfBirth(request.getDateOfBirth());
            user.setCity(request.getCity());
            user.setCountry(request.getCountry());
            user.setDescription(request.getDescription());
            user.setAvatarId(request.getAvatarId());
            user.setUserName(request.getUserName());

            entityManager.persist(user);
            return user;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateUser(Integer userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);

        if (request.getUsername() != null) {
            user.setUserName(request.getUsername());
        }
        if (request.getDateOfBirth() != null) {
            user.setDataOfBirth(request.getDateOfBirth());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getDescription() != null) {
            user.setDescription(request.getDescription());
        }
        if (request.getAvatarId() != null) {
            user.setAvatarId(request.getAvatarId());
        }

        entityManager.merge(user);
    }

    @Override
    public void deleteUser(Integer userId) {
        entityManager.createQuery("DELETE FROM RoomMember rm WHERE rm.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM RoomsRequest rr WHERE rr.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM Ban b WHERE b.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM Question q WHERE q.owner.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM Answer a WHERE a.ownerId.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM UserNotification un WHERE un.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        entityManager.createQuery("DELETE FROM BulletinBoard b WHERE b.author.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        User user = entityManager.find(User.class, userId);
        if (user != null) {
            entityManager.remove(user);
        } else {
            throw new RuntimeException("No user did not delete");
        }
    }

    @Override
    public User getUser(String login, String password) {
        try {
            String hashedPassword = sha256(password);
            return entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.login = :login AND u.password = :password",
                    User.class)
                .setParameter("login", login)
                .setParameter("password", hashedPassword)
                .getSingleResult();
        } catch (NoResultException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    public User getUserByLogin(String login) {
        try {
            return entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.login = :login",
                    User.class)
                .setParameter("login", login)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User getUserById(Integer userId) {
        return entityManager.find(User.class, userId);
    }

    public void setNotification(Integer userId, String notificationName) {
        Notification notification = notificationRepository.findByName(notificationName)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationName));
        setNotification(userId, notification);
    }

    public void setNotification(Integer userId, Notification notification) {
        User user = getUserById(userId);

        UserNotification userNotification = new UserNotification();
        userNotification.setUser(user);
        userNotification.setNotification(notification);

        entityManager.persist(userNotification);
    }

    public void removeNotification(Integer userId, String notificationName) {
        Notification notification = notificationRepository.findByName(notificationName)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationName));
        removeNotification(userId, notification);
    }

    public void removeNotification(Integer userId, Notification notification) {
        entityManager.createQuery(
                "DELETE FROM UserNotification un WHERE un.user.id = :userId AND un.notification = :notification")
            .setParameter("userId", userId)
            .setParameter("notification", notification)
            .executeUpdate();
    }

    public void updatePassword(Integer userId, String newPassword) {
        try {
            User user = getUserById(userId);
            user.setPassword(sha256(newPassword));
            entityManager.merge(user);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Room> getUserRooms(Integer userId) {
        return entityManager.createQuery(
                "SELECT rm.room FROM RoomMember rm WHERE rm.user.id = :userId",
                Room.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    private List<Notification> getUserNotifications(Integer userId) {
        return entityManager.createQuery(
                "SELECT un.notification FROM UserNotification un WHERE un.user.id = :userId",
                Notification.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    public List<Integer> getAllUsers() {
        return entityManager.createQuery("SELECT u.id FROM User u", Integer.class)
            .getResultList();
    }

    public void printAllCategories() {
        List<Category> categories = entityManager.createQuery(
                "SELECT c FROM Category c ORDER BY c.id",
                Category.class)
            .getResultList();

        System.out.println("=== Все категории из базы данных ===");
        System.out.println("ID | Название");
        System.out.println("-----------");

        if (categories.isEmpty()) {
            System.out.println("Таблица Categories пустая!");
        } else {
            for (Category category : categories) {
                System.out.printf("%-3d | %s%n", category.getId(), category.getName());
            }
        }
        System.out.println("================================");
    }
}
