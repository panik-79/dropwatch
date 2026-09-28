package com.dropwatch.flags;

import java.io.Serializable;

public record FlagInvalidationMessage(
        String flagKey,
        long timestamp
) implements Serializable {}
