package io.oxalate.backend.service;

import io.oxalate.backend.api.response.download.DownloadCertificateResponse;
import io.oxalate.backend.api.response.download.DownloadDiveResponse;
import io.oxalate.backend.api.response.download.DownloadPaymentResponse;
import io.oxalate.backend.model.Certificate;
import io.oxalate.backend.model.MemberDiveCount;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataDownloadService {
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;

    public List<DownloadCertificateResponse> downloadCertificates() {
        var certificates = certificateRepository.findAll();
        var downloadCertificateResponses = new ArrayList<DownloadCertificateResponse>();

        for (Certificate certificate : certificates) {
            var downloadCertificateResponse = certificate.toDownloadCertificateResponse();
            var optionalUser = userRepository.findById(downloadCertificateResponse.getUserId());
            var memberName = optionalUser.map(user -> user.getFirstName() + " " + user.getLastName()).orElse("Unknown");
            downloadCertificateResponse.setMemberName(memberName);
            downloadCertificateResponses.add(downloadCertificateResponse);
        }

        return downloadCertificateResponses;
    }

    public List<DownloadPaymentResponse> downloadPayments() {
        var payments = paymentRepository.findAll();
        var downloadPaymentResponses = new ArrayList<DownloadPaymentResponse>();

        for (Payment payment : payments) {
            var downloadPaymentResponse = payment.toDownloadPaymentResponse();
            var optionalUser = userRepository.findById(payment.getUserId());
            var memberName = optionalUser.map(user -> user.getFirstName() + " " + user.getLastName()).orElse("Unknown");
            downloadPaymentResponse.setName(memberName);
            downloadPaymentResponses.add(downloadPaymentResponse);
        }

        return downloadPaymentResponses;
    }

    public List<DownloadDiveResponse> downloadDives() {
        var dives = getMemberDiveCounts();
        var downloadDiveResponses = new ArrayList<DownloadDiveResponse>();

        for (MemberDiveCount memberDiveCount : dives) {
            downloadDiveResponses.add(memberDiveCount.toDownloadDiveResponse());
        }

        return downloadDiveResponses;
    }

    public List<MemberDiveCount> getMemberDiveCounts() {
        List<Object[]> results = eventRepository.getMemberDiveCount();
        List<MemberDiveCount> memberDiveCounts = new ArrayList<>();
        for (Object[] result : results) {
            long userId = ((Number) result[0]).longValue();
            String firstName = (String) result[1];
            String lastName = (String) result[2];
            int diveCount = ((Number) result[3]).intValue();
            memberDiveCounts.add(new MemberDiveCount(userId, firstName, lastName, diveCount));
        }
        return memberDiveCounts;
    }
}
