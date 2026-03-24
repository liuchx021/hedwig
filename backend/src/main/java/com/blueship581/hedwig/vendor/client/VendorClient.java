package com.blueship581.hedwig.vendor.client;

import com.blueship581.hedwig.vendor.model.*;

import java.util.List;

/** Abstraction over different CGM vendor APIs. Implementations: OttaiClient, SiSensingClient. */
public interface VendorClient {

  /** Validate whether the given access token is still valid and return token metadata. */
  VendorTokenInfo validateToken(String accessToken);

  /**
   * Perform a login using vendor credentials and return a VendorTokenInfo with the new token. Not
   * all vendors support programmatic login (e.g. Ottai requires WeChat mini-program flow).
   */
  VendorTokenInfo login(VendorLoginRequest loginRequest);

  /** Return the list of monitored subjects (followed relatives) accessible with this token. */
  List<VendorSubject> getMonitoredSubjects(String accessToken, String vendorUserId);

  /** Return the latest single glucose reading for the given subject. */
  VendorGlucoseData getLatestGlucose(
      String accessToken, String vendorUserId, VendorSubject subject);

  /** Return historical glucose readings for the given subject. */
  List<VendorGlucoseData> getHistoricalGlucose(
      String accessToken, String vendorUserId, VendorSubject subject);
}
