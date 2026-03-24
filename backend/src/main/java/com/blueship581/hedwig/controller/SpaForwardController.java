package com.blueship581.hedwig.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @GetMapping({
    "/",
    "/{path:^(?!api$|assets$)(?!.*\\..*$).*$}",
    "/{path:^(?!api$|assets$)(?!.*\\..*$).*$}/**"
  })
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
