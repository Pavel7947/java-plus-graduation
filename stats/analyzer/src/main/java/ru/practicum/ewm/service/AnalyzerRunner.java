package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    private final EventSimilarityProcessor eventSimilarityProcessor;
    private final UserActionProcessor userActionProcessor;

    @Override
    public void run(String... args) {
        Thread similarityThread = new Thread(eventSimilarityProcessor);
        similarityThread.setName("EventSimilarityProcessor");
        similarityThread.start();

        userActionProcessor.run();
    }
}
