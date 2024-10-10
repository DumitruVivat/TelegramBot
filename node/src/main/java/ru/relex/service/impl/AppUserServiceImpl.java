package ru.relex.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.relex.dao.AppUserDAO;
import ru.relex.dto.MailParams;
import ru.relex.entity.AppUser;
import ru.relex.entity.enums.UserState;
import ru.relex.service.AppUserService;
import ru.relex.utils.CryptoTool;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Log4j
@RequiredArgsConstructor
@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserDAO appUserDAO;
    private final CryptoTool cryptoTool;
    @Value("${spring.rabbitmq.queues.registration-mail}")
    private String registrationMailQueue;
    private final RabbitTemplate rabbitTemplate;

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
            var emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException e) {
            return "Please enter a valid email address. To cancel the command, enter /cancel";
        }
        var appUserOpt = appUserDAO.findByEmail(email);
        if (appUserOpt.isEmpty()) {
            appUser.setEmail(email);
            appUser.setState(UserState.BASIC_STATE);
            appUser = appUserDAO.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            sendRegistrationEmail(cryptoUserId, email);
            return "An email has been sent to your email. " +
                    "Follow the link in the email to confirm your registration.";
        } else {
            return "This email is already in use. Please enter a valid email."
                    + " To cancel the command, enter /cancel";
        }
    }

    private void sendRegistrationEmail(String cryptoUserId, String email) {
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        rabbitTemplate.convertAndSend(registrationMailQueue, mailParams);
    }
}
