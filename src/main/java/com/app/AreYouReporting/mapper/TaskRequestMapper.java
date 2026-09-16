package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.TaskRequest;
import com.app.AreYouReporting.payload.response.TaskRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskRequestMapper {

    private final UserMapper userMapper;
    private final TaskProofMapper taskProofMapper;

    public TaskRequestDto toDto(TaskRequest request) {
        if (request == null) return null;
        return TaskRequestDto.builder()
                .id(request.getId())
                .taskId(request.getTask() != null ? request.getTask().getId() : null)
                .taskTitle(request.getTask() != null ? request.getTask().getTitle() : null)
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .requestedDueDate(request.getRequestedDueDate())
                .reason(request.getReason())
                .reviewer(userMapper.toSummaryDto(request.getReviewer()))
                .reviewedAt(request.getReviewedAt())
                .reviewRemarks(request.getReviewRemarks())
                .proofs(request.getProofs() != null ? taskProofMapper.toDtoList(request.getProofs()) : Collections.emptyList())
                .createdAt(request.getCreatedAt())
                .createdBy(request.getCreatedBy())
                .build();
    }

    public List<TaskRequestDto> toDtoList(Collection<TaskRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(this::toDto).collect(Collectors.toList());
    }
}
