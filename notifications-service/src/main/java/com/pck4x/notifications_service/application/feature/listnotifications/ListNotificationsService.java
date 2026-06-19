package com.pck4x.notifications_service.application.feature.listnotifications;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.notifications_service.application.mapper.NotificationMapper;
import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListNotificationsService implements ListNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    public ListNotificationsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<QueryResult<List<NotificationResponse>>> execute(UUID userId, int page, int size) {
        var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(PageRequest.of(page, size), userId);
        var responses = NotificationMapper.INSTANCE.toResponseList(result.getResults());
        return OutputPort.ok(QueryResult.of(responses, result.getTotalCount(), result.getTotalPages(), result.getPageNumber(), result.getPageSize()));
    }
}
