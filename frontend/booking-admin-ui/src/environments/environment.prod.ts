// ═══════════════════════════════════════════════════════════
// ENVIRONMENT - PRODUCTION
// Cấu hình cho môi trường production.
// Đổi URL khi deploy. KHÔNG commit secret/prod credentials vào repo.
// ═══════════════════════════════════════════════════════════

export const environment = {
  production: true,

  // TODO: đổi thành domain thật khi deploy
  apiBaseUrl: '/api',
  apiKey: 'dev-api-key-abc123',
  pendingPaymentMinutes: 30,

  keycloak: {
    url: 'https://keycloak.smartbooking.vn', // TODO: đổi
    realm: 'booking',
    feClientId: 'smartbooking-fe-prod', // TODO: đổi
    redirectUri: 'https://smartbooking.vn/auth/callback', // TODO: đổi
  },
};
