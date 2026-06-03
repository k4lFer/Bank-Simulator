package com.pck4x.sharedcontracts.interfaces;

import com.pck4x.sharedcontracts.objects.MessageDto;

import org.springframework.http.HttpStatusCode;

import java.util.ArrayList;
import java.util.List;

public abstract class IInputValidator<T> implements IMessageDto, IHttpResponse {

    protected List<MessageDto> messageDto = new ArrayList<>();

    protected void addError(String type, String message) {
        messageDto.add(new MessageDto(type, message));
    }

    public List<MessageDto> getErrors() {
        return messageDto;
    }

    protected boolean hasErrors() {
        return !messageDto.isEmpty();
    }

    protected HttpStatusCode httpStatus;

    @Override
    public List<MessageDto> getMessage() {
        return messageDto;
    }

    @Override
    public HttpStatusCode getHttpStatusCode() {
        return httpStatus != null ? httpStatus : (hasErrors() ? HttpStatusCode.valueOf(400) : HttpStatusCode.valueOf(200));
    }

    protected void clearErrors() {
        messageDto = new ArrayList<>();
        httpStatus = null;
    }

    public abstract boolean validate(T input);
}
