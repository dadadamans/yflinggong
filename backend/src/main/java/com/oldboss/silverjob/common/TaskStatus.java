package com.oldboss.silverjob.common;

public final class TaskStatus {

    public static final String WAITING = "waiting";
    public static final String APPLYING = "applying";
    public static final String WORKING = "working";
    public static final String PENDING_PAYMENT = "pending_payment";
    public static final String DONE = "done";
    public static final String CANCELLED = "cancelled";

    private TaskStatus() {
    }
}
