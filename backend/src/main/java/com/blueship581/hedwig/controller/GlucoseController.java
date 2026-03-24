package com.blueship581.hedwig.controller;

import com.blueship581.hedwig.dto.GlucoseReadingDto;
import com.blueship581.hedwig.dto.SyncResultDto;
import com.blueship581.hedwig.service.GlucoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/glucose")
@RequiredArgsConstructor
public class GlucoseController {

  private final GlucoseService glucoseService;

  /** Get all stored readings for a subject. GET /api/glucose/subjects/{subjectId}/readings */
  @GetMapping("/subjects/{subjectId}/readings")
  public ResponseEntity<List<GlucoseReadingDto>> getReadings(@PathVariable Long subjectId) {
    return ResponseEntity.ok(glucoseService.getReadings(subjectId));
  }

  /**
   * Get the latest stored reading for a subject. GET
   * /api/glucose/subjects/{subjectId}/readings/latest
   */
  @GetMapping("/subjects/{subjectId}/readings/latest")
  public ResponseEntity<GlucoseReadingDto> getLatestReading(@PathVariable Long subjectId) {
    return ResponseEntity.ok(glucoseService.getLatestReading(subjectId));
  }

  /**
   * Trigger a live fetch of the latest reading from the vendor. POST
   * /api/glucose/connections/{connectionId}/subjects/{subjectId}/fetch/latest
   */
  @PostMapping("/connections/{connectionId}/subjects/{subjectId}/fetch/latest")
  public ResponseEntity<GlucoseReadingDto> fetchLatest(
      @PathVariable Long connectionId, @PathVariable Long subjectId) {
    return ResponseEntity.ok(glucoseService.fetchLatest(connectionId, subjectId));
  }

  /**
   * Trigger a live fetch of historical readings from the vendor (last 24h). POST
   * /api/glucose/connections/{connectionId}/subjects/{subjectId}/fetch/history
   */
  @PostMapping("/connections/{connectionId}/subjects/{subjectId}/fetch/history")
  public ResponseEntity<List<GlucoseReadingDto>> fetchHistory(
      @PathVariable Long connectionId, @PathVariable Long subjectId) {
    return ResponseEntity.ok(glucoseService.fetchHistory(connectionId, subjectId));
  }

  /**
   * Sync recent glucose readings from the vendor, overwriting existing data within the returned
   * time range. POST /api/glucose/subjects/{subjectId}/sync
   */
  @PostMapping("/subjects/{subjectId}/sync")
  public ResponseEntity<SyncResultDto> syncHistory(@PathVariable Long subjectId) {
    return ResponseEntity.ok(glucoseService.syncHistory(subjectId));
  }
}
