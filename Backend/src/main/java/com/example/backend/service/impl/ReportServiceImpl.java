package com.example.backend.service.impl;

import com.example.backend.autoMapper.SpaceMapper;
import com.example.backend.autoMapper.UserMapper;
import com.example.backend.dtos.Report.*;
import com.example.backend.entity.Rating;
import com.example.backend.entity.Report;
import com.example.backend.entity.Space;
import com.example.backend.entity.User;
import com.example.backend.repository.ReportRepository;
import com.example.backend.repository.SpaceRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;

    private final UserServiceImpl userServiceImpl;


    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository, SpaceRepository spaceRepository, UserServiceImpl userServiceImpl) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.spaceRepository = spaceRepository;
        this.userServiceImpl = userServiceImpl;
    }

    @Override
    public ReportResponse addReport(AddReportRequest addReportRequest) {
        Report report = mapReportRequestToReport(addReportRequest);
        report.setReportStatus(ReportStatus.PENDING);
        report.setReportDateTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        Report savedReport = reportRepository.save(report);
        System.out.println(savedReport);

        return mapReportToReportResponse(savedReport);
    }

    @Override
    public ReportResponse getReportById(String id) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Report not found"));
        return mapReportToReportResponse(report);
    }

    @Override
    public Page<ReportResponse> getReportsByFilters(ReportFilter reportFilter, Pageable pageable) {
        return reportRepository.findReportsByFilter(reportFilter.getReportType(), reportFilter.getReportStatus(), pageable).map(this::mapReportToReportResponse);
    }

    @Override
    public ReportResponse updateReport(UpdateReportRequest updateReportRequest) {
        Report report = reportRepository.findById(updateReportRequest.getReportId()).orElseThrow(() -> new NoSuchElementException("Report not found"));
        System.out.println(report.getReportId());
        report.setReportStatus(updateReportRequest.getReportStatus());
        reportRepository.save(report);
        return mapReportToReportResponse(report);
    }

    @Override
    public Page<ReportResponse> getMyReports(Pageable pageable) {
        User user = (User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new NoSuchElementException("User not found"));
        return reportRepository.findReportsByReporter(user, pageable).map(this::mapReportToReportResponse);
    }

    @Override
    public void deleteReport(String id) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Report not found"));
        reportRepository.delete(report);
    }

    public Report mapReportRequestToReport(AddReportRequest addReportRequest) {
        Report report = new Report();
        report.setReportType(addReportRequest.getReportType());
        report.setReportContent(addReportRequest.getReportContent());
        //report.setReporter((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        report.setReporter((User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new NoSuchElementException("User not found")));
        switch (addReportRequest.getReportType()) {
            case USER:
                Optional<User> userOptional = userRepository.findByUserId(addReportRequest.getReportedId());
                if (userOptional != null) {
                    report.setReportedUser(userOptional.get());
                } else {
                    throw new NoSuchElementException("User not found");
                }
                break;
            case SPACE:
                Optional<Space> spaceOptional = spaceRepository.findById(addReportRequest.getReportedId());
                if (spaceOptional.isPresent()) {
                    report.setReportedSpace(spaceOptional.get());
                } else {
                    throw new NoSuchElementException("Space not found");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid report type");
        }
        return report;
    }

    public ReportResponse mapReportToReportResponse(Report report) {
        return new ReportResponse(
                report.getReportId(),
                report.getReportType(),
                report.getReportStatus(),
                report.getReportContent(),
                report.getReportDateTime(),
                UserMapper.INSTANCE.userToUserResponse(report.getReporter()),
                report.getReportedUser() != null ? Optional.ofNullable(UserMapper.INSTANCE.userToUserResponse(report.getReportedUser())) : null,
                report.getReportedSpace() != null ? Optional.ofNullable(SpaceMapper.INSTANCE.spaceToSpaceResponse(report.getReportedSpace())) : null
        );
    }

}
