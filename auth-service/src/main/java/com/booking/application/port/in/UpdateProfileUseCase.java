package com.booking.application.port.in;

import com.booking.domain.model.User;

public interface UpdateProfileUseCase {
  /**
   * Phase 7: thêm field phone để user có thể bổ sung khi onboarding.
   * @param phone optional - null nghĩa là không update field này
   */
  User updateProfile(String username, String email, String timezone,
                     String phone, String firstName, String lastName);
}