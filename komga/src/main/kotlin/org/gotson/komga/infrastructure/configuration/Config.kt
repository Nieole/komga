package org.gotson.komga.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class Config {
  @Bean
  fun restClient(): RestClient = RestClient.create()
}
