package ru.relex.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.relex.dao.AppUserDAO;
import ru.relex.dao.RawDatDAO;
import ru.relex.entity.AppDocument;
import ru.relex.entity.AppPhoto;
import ru.relex.entity.AppUser;
import ru.relex.entity.RawData;
import ru.relex.exceptions.UploadFileException;
import ru.relex.service.AppUserService;
import ru.relex.service.FileService;
import ru.relex.service.MainService;
import ru.relex.service.ProducerService;
import ru.relex.service.enums.LinkType;
import ru.relex.service.enums.ServiceCommand;

import static ru.relex.entity.enums.UserState.BASIC_STATE;
import static ru.relex.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static ru.relex.service.enums.ServiceCommand.*;


@Log4j
@RequiredArgsConstructor
@Service
public class MainServiceImpl implements MainService {
    private final RawDatDAO rawDatDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;
    private final AppUserService appUserService;

    @Transactional
    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if(CANCEL.equals(serviceCommand)){
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            output = appUserService.setEmail(appUser, text);
        } else {
            log.error("Unknown user state : " + userState);
            output = "Unknown error , input cancel and try again";
        }

        var charId = update.getMessage().getChatId();
        sendAnswer(output, charId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var charId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(charId, appUser)){
            return;
        }
        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "Document was save with success," +
                    " link to download: " + link;
            sendAnswer(answer, charId);
        } catch (UploadFileException ex){
            log.error(ex);
            String error = "You can't download the file, try later";
            sendAnswer(error, charId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var charId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(charId, appUser)){
            return;
        }
        try{
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "Photo was save with success, link to download: " + link;
            sendAnswer(answer, charId);
        } catch (UploadFileException ex){
            log.error(ex);
            String error = "You can't download the photo, try later";
            sendAnswer(error, charId);
        }
    }

    private boolean isNotAllowToSendContent(Long charId, AppUser appUser) {
        var userState = appUser.getState();
        if(!appUser.getIsActive()) {
            var error = "Register or activate the account";
            sendAnswer(error, charId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "To cancel the actual command, use command /cancel";
            sendAnswer(error, charId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        var serviceCommand = ServiceCommand.fromValue(cmd);
        if (REGISTRATION.equals(serviceCommand)){
            return appUserService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Hello! To see the list of commands , type /help";
        } else {
            return "incorrect command, please input /help";
        }
    }

    private String help() {
        return "Command list:\n"
                +"/cancel - cancel the command;\n"
                +"/registration - registration the user;\n";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Command canceled";
    }

    private AppUser findOrSaveAppUser(Update update){
        var telegramUser = update.getMessage().getFrom();
        var appUserOpt = appUserDAO.findByTelegramUserId(telegramUser.getId());
        if (appUserOpt.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return appUserOpt.get();
    }

    private void saveRawData(Update update) {
        var rawData = RawData.builder()
                .event(update)
                .build();
        rawDatDAO.save(rawData);
    }
}
