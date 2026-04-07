package com.smartagri.modules.inspection.task;

import com.smartagri.modules.inspection.service.InspectionTaskService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InspectionScheduler {

    private final InspectionTaskService inspectionTaskService;

    public InspectionScheduler(InspectionTaskService inspectionTaskService) {
        this.inspectionTaskService = inspectionTaskService;
    }

    @Scheduled(
            initialDelayString = "${smart-agri.inspection.initial-delay-ms:15000}",
            fixedDelayString = "${smart-agri.inspection.fixed-delay-ms:60000}"
    )
    public void executeInspectionTask() {
        inspectionTaskService.executeScheduledInspection();
    }
}

