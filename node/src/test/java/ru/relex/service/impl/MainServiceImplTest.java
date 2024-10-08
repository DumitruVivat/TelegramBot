package ru.relex.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.relex.dao.RawDatDAO;
import ru.relex.entity.RawData;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
public class MainServiceImplTest {

    @Autowired
    private RawDatDAO rawDatDAO;

    @Test
    public void testSaveRawData() {
        Update update = new Update();
        Message msg = new Message();
        msg.setText("ololol");
        update.setMessage(msg);

        RawData rawData = RawData
                .builder()
                .event(update)
                .build();

        Set<RawData> testData = new HashSet<>();

        testData.add(rawData);
        rawDatDAO.save(rawData);

        Assert.isTrue(testData.contains(rawData), "Entity not found in the set");
    }
}
