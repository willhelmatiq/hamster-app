package com.hamsterhub.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HamsterMailService implements AlertService {
    private static final Logger log = LoggerFactory.getLogger(HamsterMailService.class);

    @Override
    public void sendAlert(String message) {
        log.warn("ALERT: {}", message);
    }
}