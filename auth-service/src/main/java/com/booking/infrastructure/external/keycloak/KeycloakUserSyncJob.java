//package com.booking.infrastructure.external.keycloak;
//
//import com.booking.application.port.out.KeycloakAdminPort;
//import com.booking.application.port.out.UserRepositoryPort;
//import com.booking.application.port.out.UserKcLinkRepositoryPort;
//import com.booking.domain.model.User;
//import com.booking.domain.model.UserKcLink;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.time.ZonedDateTime;
//import java.util.List;
//
///**
// * Startup job: tự động sync users từ auth DB → Keycloak.
// *
// * Chạy 1 lần khi app khởi động (ApplicationReadyEvent).
// * - Tìm users chưa có KC link (chưa sync)
// * - Tạo user trong KC + lưu link
// * - Non-blocking: lỗi sync 1 user không ảnh hưởng user khác
// *
// * Sau khi fix Keycloak persistence (PostgreSQL), job này chỉ cần
// * chạy cho users cũ. Users mới register sẽ tự sync qua AuthServiceImpl.
// */
//@Component
//@RequiredArgsConstructor
//@Log4j2
//public class KeycloakUserSyncJob {
//
//    private final UserRepositoryPort userRepository;
//    private final UserKcLinkRepositoryPort kcLinkRepository;
//    private final KeycloakAdminPort kcAdminClient;
//
//    @EventListener(ApplicationReadyEvent.class)
//    @Async
//    public void syncUsersToKeycloak() {
//        log.info("[KC Sync] Starting user sync check...");
//
//        try {
//            List<User> allUsers = userRepository.findAll();
//            int synced = 0;
//            int skipped = 0;
//            int failed = 0;
//
//            for (User user : allUsers) {
//                try {
//                    // Check if already has KC link
//                    if (kcLinkRepository.existsByUserId(user.getId())) {
//                        skipped++;
//                        continue;
//                    }
//
//                    // Check if user already exists in KC by email
//                    KeycloakAdminPort.KcUserInfo existing =
//                            kcAdminClient.findUserByEmail(user.getEmail());
//
//                    String kcUserId;
//                    if (existing != null) {
//                        // User exists in KC but no link → create link only
//                        kcUserId = existing.id();
//                        log.info("[KC Sync] Found existing KC user for {}, linking",
//                                user.getUsername());
//                    } else {
//                        // User not in KC → create with temporary password
//                        // Note: KC password will be separate from auth DB password
//                        // User can reset via "forgot password" or login via auth-service
//                        kcUserId = kcAdminClient.createUser(
//                                user.getUsername(),
//                                user.getEmail(),
//                                generateTempPassword(),
//                                user.isEmailVerified()
//                        );
//                        log.info("[KC Sync] Created KC user: {} → {}",
//                                user.getUsername(), kcUserId);
//                    }
//
//                    // Save KC link
//                    UserKcLink link = new UserKcLink();
//                    link.setUserId(user.getId());
//                    link.setKcUserId(kcUserId);
//                    link.setKcProvider(null);
//                    link.setAuthSource("LINKED");
//                    link.setKcSyncedAt(ZonedDateTime.now());
//                    link.setSyncStatus("SYNCED");
//                    link.setSyncVersion(1L);
//                    kcLinkRepository.save(link);
//
//                    synced++;
//
//                } catch (Exception e) {
//                    failed++;
//                    log.warn("[KC Sync] Failed to sync user {}: {}",
//                            user.getUsername(), e.getMessage());
//                    // Continue with next user — don't stop the whole sync
//                }
//            }
//
//            log.info("[KC Sync] Completed: total={}, synced={}, skipped={}, failed={}",
//                    allUsers.size(), synced, skipped, failed);
//
//        } catch (Exception e) {
//            log.error("[KC Sync] Sync job failed: {}", e.getMessage(), e);
//            // Non-blocking: app continues running even if sync fails
//        }
//    }
//
//    /**
//     * Generate a temporary password for KC user.
//     * User won't use this directly — they login via auth-service (/api/auth/login)
//     * which uses the password from auth DB.
//     * KC password is only needed for SSO login flow.
//     */
//    private String generateTempPassword() {
//        return "TempSync_" + System.currentTimeMillis() + "!";
//    }
//}