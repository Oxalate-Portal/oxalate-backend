package io.oxalate.backend.model;

import io.oxalate.backend.api.response.BlockedDateResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blocked_dates")
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "blocked_date", nullable = false)
    private Date blockedDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "creator", nullable = false)
    private long creator;

    public BlockedDateResponse toResponse() {
        return BlockedDateResponse.builder()
                                  .id(this.id)
                                  .blockedDate(this.blockedDate)
                                  .createdAt(this.createdAt)
                                  .creator(this.creator)
                                  .build();
    }
}
