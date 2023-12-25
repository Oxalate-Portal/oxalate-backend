package io.oxalate.backend.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EventsParticipantId implements Serializable {
    private long userId;

    private long eventId;
}