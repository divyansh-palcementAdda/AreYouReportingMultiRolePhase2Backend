package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.TaskRequestReviewRequest;
import com.app.AreYouReporting.payload.request.TaskRequestSubmitRequest;
import com.app.AreYouReporting.payload.response.TaskRequestDto;

import java.util.List;
import java.util.UUID;

public interface TaskRequestService {

    TaskRequestDto submitRequest(UUID taskId, TaskRequestSubmitRequest request);

    TaskRequestDto reviewRequest(UUID requestId, TaskRequestReviewRequest reviewRequest);

    List<TaskRequestDto> getRequestsByTaskId(UUID taskId);

    TaskRequestDto getRequestById(UUID requestId);

    void cancelRequest(UUID requestId);
}
