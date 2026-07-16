// ═══════════════════════════════════════════════════════════
// ENVIRONMENT - DEV (mặc định)
// Cấu hình cho môi trường local development.
// Khi build production, Angular sẽ dùng environment.prod.ts (file replacement).
// ═══════════════════════════════════════════════════════════

export const environment = {
  production: false,

  // BE base URL (auth-service đang chạy port 8081, context-path /api)
  apiBaseUrl: '/api',
  apiKey: 'dev-api-key-abc123',

  // Keycloak (Form B SSO)
  // Phải khớp với BE application.properties:
  //   app.keycloak.url=http://localhost:8180
  //   app.keycloak.realm=booking
  //   app.keycloak.fe-client-id=smartbooking-fe
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'booking', // ← QUAN TRỌNG: phải là 'booking' (match BE), không phải 'smartbooking'
    feClientId: 'booking-frontend',
    redirectUri: 'http://localhost:4200/auth/callback',
  },
};
