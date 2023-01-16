package com.wallet.unhandled_exception.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wallet.unhandled_exception.exceptions.SSOFailureException;
import com.wallet.unhandled_exception.model.LdapRequest;
import com.wallet.unhandled_exception.model.UserInfo;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.Date;

@Service
public class SSOAuthUserServiceImpl {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ldapUserDetailsApi = "https://jssostgpvt.indiatimes.com/sso/ldap/identity/getPublicProfile";
    private static final String ldapPublicUserDetails = "https://jssostgpvt.indiatimes.com/sso/ldap/identity/getPublicProfile";

    public UserInfo getUserDetailsIfTokenValid(String ssoToken) throws SSOFailureException {
        UserInfo userInfo = null;

        LdapRequest ldapRequest = new LdapRequest();
        ldapRequest.setAccessToken(ssoToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LdapRequest> entity = new HttpEntity<>(ldapRequest, headers);


        ResponseEntity<JsonNode> responseNew = restTemplate.exchange(ldapUserDetailsApi, HttpMethod.POST, entity, JsonNode.class);
        if (responseNew != null && responseNew.getStatusCode() != null && responseNew.getStatusCode().value() == 200 && responseNew.getBody() != null) {

            if (responseNew.getBody().get("code") != null && responseNew.getBody().get("code").asInt() == 200 && responseNew.getBody().get("data") != null) {
                userInfo = new UserInfo();
                JsonNode jsonNode = responseNew.getBody().get("data");
                if (jsonNode.get("firstName") != null) {
                    userInfo.setFirstName(jsonNode.get("firstName").asText());
                }
                if (jsonNode.get("lastName") != null) {
                    userInfo.setLastName(jsonNode.get("lastName").asText());
                }
                if (jsonNode.get("email") != null) {
                    userInfo.setEmail(jsonNode.get("email").asText());
                }
                if (jsonNode.get("sapId") != null) {
                    userInfo.setSapId(jsonNode.get("sapId").asText());
                }
                if (jsonNode.get("expiry") != null) {
                    userInfo.setExpiryTime(jsonNode.get("expiry").asText());
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, 30);
                    userInfo.setExpiryTime(String.valueOf(cal.getTime()));
                }
                userInfo.setSsoToken(ssoToken);

            }
        }

        if (userInfo != null && isSSOTicketValid(userInfo.getExpiryTime())) {
            return userInfo;
        }
        throw new SSOFailureException("User session is not valid", true);
    }


    public UserInfo getUserPublicProfile(String emailId) throws SSOFailureException {
        UserInfo userInfo = null;

        LdapRequest ldapRequest = new LdapRequest();
        ldapRequest.setEmail(emailId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LdapRequest> entity = new HttpEntity<>(ldapRequest, headers);
        try {
            ResponseEntity<JsonNode> responseNew = restTemplate.exchange(ldapPublicUserDetails, HttpMethod.POST, entity, JsonNode.class);

            if (responseNew != null && responseNew.getStatusCode() != null && responseNew.getStatusCode().value() == 200 && responseNew.getBody() != null) {

                if (responseNew.getBody().get("code") != null && responseNew.getBody().get("code").asInt() == 200 && responseNew.getBody().get("data") != null) {
                    userInfo = new UserInfo();
                    JsonNode jsonNode = responseNew.getBody().get("data");
                    if (jsonNode.get("firstName") != null) {
                        userInfo.setFirstName(jsonNode.get("firstName").asText());
                    }
                    if (jsonNode.get("lastName") != null) {
                        userInfo.setLastName(jsonNode.get("lastName").asText());
                    }
                    if (jsonNode.get("email") != null) {
                        userInfo.setEmail(jsonNode.get("email").asText());
                    }

                } else if (responseNew.getBody().get("code") != null && responseNew.getBody().get("code").asInt() == 402) {
                    throw new SSOFailureException(emailId + " is not a valid LDAP user");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new SSOFailureException(ex.getMessage());
        }

        return userInfo;

    }

    private boolean isSSOTicketValid(String date) {
        if (date != null) {
            Date now = new Date();
            return now.before(new Date(Long.parseLong(date)));

        }
        return false;
    }

}