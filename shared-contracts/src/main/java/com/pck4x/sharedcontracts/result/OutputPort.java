package com.pck4x.sharedcontracts.result;

import com.pck4x.sharedcontracts.interfaces.IHttpResponse;
import com.pck4x.sharedcontracts.interfaces.IMessageDto;
import com.pck4x.sharedcontracts.objects.MessageDto;
import org.springframework.http.HttpStatusCode;

import java.util.Collections;
import java.util.List;

public class OutputPort<T> implements IHttpResponse, IMessageDto {

    private T data = null;
    private boolean success;
    private HttpStatusCode status;
    private List<MessageDto> messages = Collections.emptyList();

    public OutputPort() {
    }

    public OutputPort(T data, boolean success, HttpStatusCode status, List<MessageDto> messages) {
        this.data = data;
        this.success = success;
        this.status = status;
        this.messages = messages != null ? messages : Collections.emptyList();
    }

    @Override
    public HttpStatusCode getHttpStatusCode() {
        return status;
    }

    @Override
    public List<MessageDto> getMessage() {
        return messages;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public void setStatus(HttpStatusCode status) {
        this.status = status;
    }

    public List<MessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }

    public static <T> OutputPort<T> ok(T data) {
        return new OutputPort<>(data, true, HttpStatusCode.valueOf(200), Collections.emptyList());
    }

    public static <T> OutputPort<T> ok(T data, String message) {
        return new OutputPort<>(
                data, true, HttpStatusCode.valueOf(200),
                Collections.singletonList(new MessageDto("SUCCESS", message))
        );
    }

    public static <T> OutputPort<T> created(T data, String message) {
        return new OutputPort<>(
                data, true, HttpStatusCode.valueOf(201),
                Collections.singletonList(new MessageDto("SUCCESS", message))
        );
    }

    public static <T> OutputPort<T> noContent() {
        return new OutputPort<>(null, true, HttpStatusCode.valueOf(204), Collections.emptyList());
    }

    public static <T> OutputPort<T> badRequest(String message) {
        return new OutputPort<>(
                null, false, HttpStatusCode.valueOf(400),
                Collections.singletonList(new MessageDto("ERROR", message))
        );
    }

    public static <T> OutputPort<T> notFound(String message) {
        return new OutputPort<>(
                null, false, HttpStatusCode.valueOf(404),
                Collections.singletonList(new MessageDto("ERROR", message))
        );
    }

    public static <T> OutputPort<T> conflict(String message) {
        return new OutputPort<>(
                null, false, HttpStatusCode.valueOf(409),
                Collections.singletonList(new MessageDto("ERROR", message))
        );
    }

    public static <T> OutputPort<T> failure(HttpStatusCode status, String message) {
        return new OutputPort<>(
                null, false, status,
                Collections.singletonList(new MessageDto("ERROR", message))
        );
    }

    public static <T> OutputPort<T> failures(HttpStatusCode status, List<MessageDto> messages) {
        return new OutputPort<>(null, false, status, messages);
    }
}
