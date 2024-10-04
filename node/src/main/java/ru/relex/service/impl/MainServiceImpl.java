package ru.relex.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.relex.dao.AppUserDAO;
import ru.relex.dao.RawDatDAO;
import ru.relex.entity.AppUser;
import ru.relex.entity.RawData;
import ru.relex.service.MainService;
import ru.relex.service.ProducerService;

import static ru.relex.entity.enums.UserState.BASIC_STATE;
import static ru.relex.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static ru.relex.service.enums.ServiceCommands.*;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDatDAO rawDatDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    public MainServiceImpl(RawDatDAO rawDatDAO, ProducerService producerService, AppUserDAO appUserDAO) {
        this.rawDatDAO = rawDatDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
    }
    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        if(CANCEL.equals(text)){
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            // TODO 
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
        var answer = "Document was save with success, link to download http://test.md/get-doc/777";
        sendAnswer(answer, charId);
    }

    private boolean isNotAllowToSendContent(Long charId, AppUser appUser) {
        var userState = appUser.getState();
        if(!appUser.getIsActive()) {
            var error = "Register of activate the account";
            sendAnswer(error, charId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Cancel the actual command with command /cancel";
            sendAnswer(error, charId);
            return true;
        }
        return false;
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var charId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(charId, appUser)){
            return;
        }
        var answer = "Photo was save with success, link to download http://test.md/get-photo/777";
        sendAnswer(answer, charId);
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if (REGISTRATION.equals(cmd)){
            //TODO
            return "temporary forbidden";
        } else if (HELP.equals(cmd)) {
            return help();
        } else if (START.equals(cmd)) {
            return "Hello! you can see the command list , input /help";
        } else {
            return "incorrect command, please input /help";
        }
    }

    private String help() {
        return "Command list:\n"+"/cancel - cancel the command\n" +"/registration - registration the user\n";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Command canceled";
    }

    private AppUser findOrSaveAppUser(Update update){
        User telegramUser = update.getMessage().getFrom();
        AppUser persistebtAppUser = appUserDAO.findByTelegramUserId(telegramUser.getId());
        if (persistebtAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return persistebtAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDatDAO.save(rawData);
    }
}
