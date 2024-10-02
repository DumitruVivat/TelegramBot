package ru.relex.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.relex.dao.RawDatDAO;
import ru.relex.entity.RawData;
import ru.relex.service.MainService;
import ru.relex.service.ProducerService;

@Service
public class MainServiceImpl implements MainService {
    private final RawDatDAO rawDatDAO;
    private final ProducerService producerService;
    public MainServiceImpl(RawDatDAO rawDatDAO, ProducerService producerService) {
        this.rawDatDAO = rawDatDAO;
        this.producerService = producerService;
    }
    @Override
    public void processTextUpdate(Update update) {
        saveRawData(update);

        var message = update.getMessage();
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Hello from NODE");
        producerService.produceAnswer(sendMessage);
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDatDAO.save(rawData);
    }
}
