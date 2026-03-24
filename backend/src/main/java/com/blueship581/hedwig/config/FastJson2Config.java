package com.blueship581.hedwig.config;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class FastJson2Config implements WebMvcConfigurer {

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

    FastJsonConfig config = new FastJsonConfig();
    config.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    config.setWriterFeatures(
        JSONWriter.Feature.WriteEnumsUsingName,
        JSONWriter.Feature.WriteNullListAsEmpty,
        JSONWriter.Feature.WriteNullStringAsEmpty,
        JSONWriter.Feature.WriteNullBooleanAsFalse);
    converter.setFastJsonConfig(config);
    converter.setDefaultCharset(StandardCharsets.UTF_8);
    converter.setSupportedMediaTypes(
        List.of(MediaType.APPLICATION_JSON, new MediaType("application", "*+json")));

    converters.add(0, converter);
  }
}
