<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed; section>
    <#if section = "header">
    <#elseif section = "form">
        <div class="booking-page">
            <div class="hero-content">

            <span class="hero-badge">
                HOTEL RESERVATION SYSTEM
            </span>

                    <h1>
                        Book your
                        <br>
                        perfect stay.
                    </h1>

                    <p>
                        Experience luxury hotels and seamless booking with
                        SmartBooking.
                    </p>

                    <div class="hero-list">
                        <div>✓ 5000+ Hotels</div>
                        <div>✓ Best Price Guarantee</div>
                        <div>✓ Secure Payment</div>
                        <div>✓ 24/7 Support</div>
                    </div>

            </div>
            <div class="booking-card">
                <div class="card-logo">
                    <div class="logo-icon">
                        <img src="${url.resourcesPath}/img/Logo.jpg" alt="SmartBooking" />
                    </div>
                    <div class="logo-text">
                        <span class="logo-name">SmartBooking</span>
                        <span class="logo-sub">Hotel Reservation System</span>
                    </div>
                </div>

                <h2 class="card-title">Chào mừng trở lại</h2>
                <p class="card-subtitle">Đăng nhập để quản lý đặt phòng của bạn</p>

                <#assign usernameValue = "">
                <#if login?? && login.username?? && login.username?has_content>
                    <#assign usernameValue = login.username>
                <#elseif auth?? && auth.attemptedUsername?? && auth.attemptedUsername?has_content>
                    <#assign usernameValue = auth.attemptedUsername>
                <#elseif attemptedUsername?? && attemptedUsername?has_content>
                    <#assign usernameValue = attemptedUsername>
                </#if>

                <#assign isReauthFlow = false>
                <#if auth?? && auth.attemptedUsername?? && auth.attemptedUsername?has_content>
                    <#assign isReauthFlow = auth.showUsername()>
                </#if>

                <#if isReauthFlow>
                    <div class="preauth-panel">
                        <button type="button" class="preauth-account" onclick="bookingReauthAccountContinue()">
                            <span class="preauth-copy">
                                <span class="preauth-eyebrow">X&aacute;c th&#7921;c l&#7841;i</span>
                                <strong>${kcSanitize(usernameValue)?no_esc}</strong>
                                <span>Ch&#7885;n t&agrave;i kho&#7843;n n&agrave;y &#273;&#7875; ti&#7871;p t&#7909;c.</span>
                            </span>
                        </button>
                        <a href="${url.loginRestartFlowUrl}" class="preauth-switch">&#272;&#7893;i t&agrave;i kho&#7843;n</a>
                    </div>
                </#if>

                <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <#assign suppressReauthInfoMessage = false>
                    <#if isReauthFlow && message.type = 'info' && message.summary?lower_case?contains("re-authenticate")>
                        <#assign suppressReauthInfoMessage = true>
                    </#if>
                    <#if suppressReauthInfoMessage == false>
                        <div class="alert alert-${message.type}">
                            <#if message.type = 'error' || message.type = 'info'>
                                <svg class="alert-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 4v4M8 10v1"/></svg>
                            </#if>
                            ${kcSanitize(message.summary)?no_esc}
                        </div>
                    </#if>
                </#if>

                <#if isReauthFlow>
                    <script>
                        function bookingReauthAccountContinue() {
                            var password = document.getElementById('password');
                            var form = document.getElementById('kc-form-login');
                            if (password && password.value && form) {
                                if (form.requestSubmit) {
                                    form.requestSubmit();
                                } else {
                                    form.submit();
                                }
                                return;
                            }
                            if (password) {
                                password.focus();
                            }
                        }
                        document.addEventListener('DOMContentLoaded', function () {
                            var password = document.getElementById('password');
                            if (password) {
                                password.focus();
                            }
                        });
                    </script>
                        </#if>

                <form id="kc-form-login" action="${url.loginAction}" method="post">
                    <div class="field">
                        <label for="username">
                            <#if !realm.loginWithEmailAllowed>Tên đăng nhập<#elseif !realm.registrationEmailAsUsername>Email hoặc tên đăng nhập<#else>Email</#if>
                        </label>
                        <div class="field-input">
                            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8a9a6e" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                            <input id="username" name="username" value="${usernameValue}" type="text" autofocus autocomplete="username" placeholder="you@example.com" <#if usernameEditDisabled??>readonly</#if> />
                            <#if usernameEditDisabled?? && usernameValue?has_content>
                                <input type="hidden" name="username" value="${usernameValue}" />
                            </#if>
                        </div>
                    </div>

                    <div class="field">
                        <div class="field-header">
                            <label for="password">Mật khẩu</label>
                            <#if realm.resetPasswordAllowed>
                                <a href="${url.loginResetCredentialsUrl}" class="field-link">Quên mật khẩu?</a>
                            </#if>
                        </div>
                        <div class="field-input">
                            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#8a9a6e" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                            <input id="password" name="password" type="password" autocomplete="current-password" placeholder="Nhập mật khẩu" />
                        </div>
                    </div>

                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <div class="remember">
                            <input id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
                            <label for="rememberMe">Ghi nhớ đăng nhập</label>
                        </div>
                    </#if>

                    <input type="hidden" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <button type="submit" id="kc-login">Đăng nhập</button>
                </form>

                <#if realm.password && social.providers??>
                    <div class="divider"><span>hoặc tiếp tục với</span></div>
                    <div class="social-list">
                        <#list social.providers as p>
                            <a class="social-btn" href="${p.loginUrl}">
                                <#if p.alias == "google">
                                    <svg width="18" height="18" viewBox="0 0 24 24">
                                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                                    </svg>
                                    Google
                                <#else>
                                    ${p.displayName!}
                                </#if>
                            </a>
                        </#list>
                    </div>
                </#if>

                <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                    <p class="register">Chưa có tài khoản? <a href="${url.registrationUrl}">Đăng ký ngay</a></p>
                </#if>

                <div class="footer">
                    <p>&copy; 2026 SmartBooking. All rights reserved.</p>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
