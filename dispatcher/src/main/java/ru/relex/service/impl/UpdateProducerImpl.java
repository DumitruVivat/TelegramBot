package ru.relex.service.impl;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.relex.service.UpdateProducer;

public class UpdateProducerImpl implements UpdateProducer {
    @Override
    public void produce(String rabbitQueue, Update update) {

    }
}
