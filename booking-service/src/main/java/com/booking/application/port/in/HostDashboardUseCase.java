package com.booking.application.port.in;

import com.booking.presentation.response.HostDashboardResponse;

import java.util.UUID;

public interface HostDashboardUseCase {

    HostDashboardResponse getDashboard(UUID ownerUserId);
}
