package edu.handong.csee.histudy.config;

import edu.handong.csee.histudy.service.DiscordClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Slf4j
@Configuration
public class HttpClientConfig {

  @Value("${custom.webhook.discord:}")
  private String discordWebhookUrlOr;

  @Value("${custom.webhook.discord-timeout-ms:3000}")
  private int discordWebhookTimeoutMs;

  @Bean
  public DiscordClient discordClient() {
    if (!StringUtils.hasText(discordWebhookUrlOr)) {
      return createNoOpClient();
    }
    try {
      SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
      requestFactory.setConnectTimeout(discordWebhookTimeoutMs);
      requestFactory.setReadTimeout(discordWebhookTimeoutMs);
      RestClient client =
          RestClient.builder()
              .baseUrl(discordWebhookUrlOr)
              .requestFactory(requestFactory)
              .build();
      RestClientAdapter adapter = RestClientAdapter.create(client);
      HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

      log.info("Discord webhook client configured successfully");
      return factory.createClient(DiscordClient.class);
    } catch (Exception e) {
      log.error("Failed to create Discord client, falling back to no-op client.", e);
      return createNoOpClient();
    }
  }

  private DiscordClient createNoOpClient() {
    return __ -> log.debug("Discord notification skipped");
  }
}
