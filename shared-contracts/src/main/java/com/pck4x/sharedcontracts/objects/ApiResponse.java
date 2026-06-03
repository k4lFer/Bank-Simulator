package com.pck4x.sharedcontracts.objects;

import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private List<MessageDto> messages;

    public ApiResponse(boolean success, T data, List<MessageDto> messages) {
        this.success = success;
        this.data = data;
        this.messages = messages;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public List<MessageDto> getMessages() {
        return messages;
    }
}
