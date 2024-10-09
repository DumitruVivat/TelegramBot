package ru.relex.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.relex.dao.AppUserDAO;
import ru.relex.dto.MailParams;
import ru.relex.entity.AppUser;
import ru.relex.entity.enums.UserState;
import ru.relex.service.AppUserService;
import ru.relex.utils.CryptoTool;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Log4j
@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserDAO appUserDAO;
    private final CryptoTool cryptoTool;
    @Value("${service.mail.uri}")
    private String mailServiceUri;

    public AppUserServiceImpl(AppUserDAO appUserDAO, CryptoTool cryptoTool) {
        this.appUserDAO = appUserDAO;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()) {
            return "You already registered";
        } else if (appUser.getEmail() != null) {
            return "An email has already been sent to your email. "
                    + "Follow the link in the email to confirm your registration.";
        }
        appUser.setState(UserState.WAIT_FOR_EMAIL_STATE);
        appUserDAO.save(appUser);
        return "Enter your email:";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException e) {
            return "Please enter a valid email address. To cancel the command, enter /cancel";
        }
        var optional = appUserDAO.findByEmail(email);
        if (optional.isEmpty()) {
            appUser.setEmail(email);
            appUser.setState(UserState.BASIC_STATE);
            appUser = appUserDAO.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var msg = String.format("Sending email to %s failed.", email);
                log.error(msg);
                appUser.setEmail(null);
                appUserDAO.save(appUser);
                return msg;
            }
            return "An email has been sent to your email. " +
                    "Follow the link in the email to confirm your registration.";
        } else {
            return "This email is already in use. Please enter a valid email."
                    + " To cancel the command, enter /cancel";
        }
    }

    private ResponseEntity<?> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<MailParams>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri,
                HttpMethod.POST,
                request,
                String.class);
    }
}
