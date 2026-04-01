package softarch.restaurant.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import softarch.restaurant.domain.report.entity.Report;
import softarch.restaurant.domain.report.entity.ReportType;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByTypeOrderByCreatedAtDesc(ReportType type);
}