package io.oxalate.backend.repository;

import java.time.Instant;

public interface MessageWithReadStatus {
    long getId();

    String getDescription();

    String getTitle();

    String getMessage();

    long getCreator();

    Instant getCreatedAt();

    boolean getRead();
}
