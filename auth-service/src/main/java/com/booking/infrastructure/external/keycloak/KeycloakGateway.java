package com.booking.infrastructure.external.keycloak;


import com.booking.infrastructure.config.AppProperties;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@Component
@RequiredArgsConstructor
@Log4j2
public class KeycloakGateway {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    /**
     * Gọi Keycloak để xác thực username/password
     * Dùng ROPC grant (Resource Owner Password Credentials)
     *
     * @return true nếu KC xác nhận đúng
     */

    public boolean authenticate(String username, String password){
        String tokenUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/token",
                appProperties.getKeycloak().getUrl(),
                appProperties.getKeycloak().getRealm()
        );

        log.info("[KC] Authenticating user: {}", MaskUtil.maskUsername(username));

        //KC dung form data khong phai json
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "password");
        form.add("client_id", appProperties.getKeycloak().getClientId());
        form.add("client_secret", appProperties.getKeycloak().getClientSecret());
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try{
            ResponseEntity<String> response = restTemplate.postForEntity(
                    tokenUrl,
                    new HttpEntity<>(form, headers),
                    String.class
            );

            boolean ok = response.getStatusCode() == HttpStatus.OK;
            log.info("[KC] Authentication result for user {}: {}", MaskUtil.maskUsername(username), ok ? "OK" : "FAIL");
            return ok;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("[KC] Wrong credentials for: {}", MaskUtil.maskUsername(username));
            return false;
        } catch (Exception e) {
            log.error("[KC] Error calling Keycloak: {}", e.getMessage());
            return false;
        }
    }


}
