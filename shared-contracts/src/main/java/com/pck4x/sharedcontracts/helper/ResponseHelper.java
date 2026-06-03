package com.pck4x.sharedcontracts.helper;

import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import org.springframework.http.ResponseEntity;

public class ResponseHelper {

    public static <T> ResponseEntity<ApiResponse<T>> toResponse(OutputPort<T> result) {
        int status = result.getStatus().value();

        if (status == 204) {
            return ResponseEntity.noContent().build();
        }

        var body = new ApiResponse<>(
                result.isSuccess(),
                result.getData(),
                result.getMessages()
        );

        return ResponseEntity.status(status).body(body);
    }
}
