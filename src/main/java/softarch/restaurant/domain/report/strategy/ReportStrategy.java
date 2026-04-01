package softarch.restaurant.domain.report.strategy;

import softarch.restaurant.domain.report.dto.reportDTOs.ReportData;
import softarch.restaurant.domain.report.dto.reportDTOs.ReportRequest;
import softarch.restaurant.domain.report.entity.ReportType;
import softarch.restaurant.orchestration.ReportingDataFacade;

/**
 * Matches diagram: ReportStrategy interface.
 * Each concrete strategy handles one ReportType and fetches
 * raw data via ReportingDataFacade.
 */
public interface ReportStrategy {

    // Matches diagram: isSupported(type: ReportType): Boolean
    boolean isSupported(ReportType type);

    // Matches diagram: execute(req, dataFacade): ReportData
    ReportData execute(ReportRequest request, ReportingDataFacade dataFacade);
}