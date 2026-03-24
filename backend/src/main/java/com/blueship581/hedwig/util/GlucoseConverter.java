package com.blueship581.hedwig.util;

import org.springframework.stereotype.Component;

@Component
public class GlucoseConverter {

  private static final double MMOL_TO_MGDL_FACTOR = 18.0182;

  public double mmolToMgdl(double mmol) {
    return Math.round(mmol * MMOL_TO_MGDL_FACTOR);
  }
}
