package com.pck4x.users_service.application.port.output;

import java.util.List;

public interface Mapper<D, R> {
    R toResponse(D domain);
    List<R> toResponseList(List<D> domainList);
}
