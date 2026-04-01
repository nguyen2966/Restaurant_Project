package softarch.restaurant.domain.report.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Matches diagram: Report entity — stores a record of each generated report.
 */
@Entity
@Table(name = "report_history")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // URL to exported file (CSV/PDF), null if not exported
    @Column(name = "file_url", length = 500)
    private String fileUrl;

    protected Report() {}

    public Report(ReportType type, LocalDateTime startDate,
                  LocalDateTime dueDate, String fileUrl) {
        this.type      = type;
        this.startDate = startDate;
        this.dueDate   = dueDate;
        this.fileUrl   = fileUrl;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId()                  { return id; }
    public ReportType getType()          { return type; }
    public LocalDateTime getStartDate()  { return startDate; }
    public LocalDateTime getDueDate()    { return dueDate; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public String getFileUrl()           { return fileUrl; }
    public void setFileUrl(String url)   { this.fileUrl = url; }
}