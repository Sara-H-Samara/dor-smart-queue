package com.sara.backend.streams;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class QueueChangeAnnouncer {

    private final ApplicationEventPublisher eventPublisher;

    public QueueChangeAnnouncer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void announceTicketChanged() {
        eventPublisher.publishEvent(new QueueChangedEvent(QueueChangeKind.TICKET_CHANGED));
    }

    public void announceConfigurationChanged() {
        eventPublisher.publishEvent(new QueueChangedEvent(QueueChangeKind.CONFIGURATION_CHANGED));
    }
}
