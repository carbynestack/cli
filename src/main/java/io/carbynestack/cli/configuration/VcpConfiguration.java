/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.castor.common.CastorServiceUri;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.URI;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import static io.carbynestack.cli.configuration.ConfigurationCommand.CONFIGURATION_MESSAGE_BUNDLE;
import static io.carbynestack.cli.util.ConsoleReader.readOrDefault;

@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VcpConfiguration {

  static final String AMPHORA_URL_ENV_KEY_FORMAT = "CS_VCP_{0}_AMPHORA_URL";
  static final String CASTOR_URL_ENV_KEY_FORMAT = "CS_VCP_{0}_CASTOR_URL";
  static final String EPHEMERAL_URL_ENV_KEY_FORMAT = "CS_VCP_{0}_EPHEMERAL_URL";
  static final String OAUTH2_AUTH_ENDPOINT_URI = "CS_VCP_{0}_OAUTH2_AUTH_ENDPOINT_URI";
  static final String OAUTH2_TOKEN_ENDPOINT_URI = "CS_VCP_{0}_OAUTH2_TOKEN_ENDPOINT_URI";
  static final String OAUTH2_CLIENT_ID_ENV_KEY_FORMAT = "CS_VCP_{0}_OAUTH2_CLIENT_ID";
  static final String OAUTH2_CALLBACK_URL_ENV_KEY_FORMAT = "CS_VCP_{0}_OAUTH2_CALLBACK_URL";
  static final String AMPHORA_URL_FORMAT = "{0}amphora";
  static final String CASTOR_URL_FORMAT = "{0}castor";
  static final String EPHEMERAL_URL_FORMAT = "{0}";

  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle(CONFIGURATION_MESSAGE_BUNDLE);
  URI baseUrl;
  AmphoraServiceUri amphoraServiceUri;
  CastorServiceUri castorServiceUri;
  URI ephemeralServiceUrl;
  String oAuth2clientId;
  URI oAuth2AuthEndpointUri;
  URI oAuth2TokenEndpointUri;
  URI oAuth2CallbackUrl;
  @JsonProperty(value = "id", required = true, index = 10)
  private int providerNumber;

  VcpConfiguration(int providerNumber) {
    this.providerNumber = providerNumber;
  }

  void configure() throws CsCliConfigurationException {
    try {
      System.out.printf("Configuring Provider #%d%n", providerNumber);
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.base-url"), getActualBaseUrl()));
      updateBaseUrlIfChanged(getActualBaseUrl(), readOrDefault(getActualBaseUrl()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.amphora-service-url"),
              getActualAmphoraServiceUri()));
      amphoraServiceUri = new AmphoraServiceUri(readOrDefault(getActualAmphoraServiceUri()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.castor-service-url"),
              getActualCastorServiceUri()));
      castorServiceUri = new CastorServiceUri(readOrDefault(getActualCastorServiceUri()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.ephemeral-service-url"),
              getActualEphemeralServiceUrl()));
      ephemeralServiceUrl = URI.create(readOrDefault(getActualEphemeralServiceUrl()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.oauth2-client-id"),
              getActualOAuth2ClientId()));
      oAuth2clientId = readOrDefault(getActualOAuth2ClientId());
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.oauth2-auth-endpoint-uri"),
              getOauth2AuthEndpointUri()));
      oAuth2AuthEndpointUri = URI.create(readOrDefault(getActualOauth2AuthEndpointUri()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.oauth2-token-endpoint-uri"),
              getOauth2AuthEndpointUri()));
      oAuth2TokenEndpointUri = URI.create(readOrDefault(getActualOauth2TokenEndpointUri()));
      System.out.printf(
          "\t%s",
          MessageFormat.format(
              MESSAGES.getString("configuration.request.vcp.oauth2-callback-url"),
              getActualOAuth2CallbackUrl()));
      oAuth2CallbackUrl = URI.create(readOrDefault(getActualOAuth2CallbackUrl()));
    } catch (NoSuchElementException | IllegalStateException | IllegalArgumentException e) {
      throw new CsCliConfigurationException(MESSAGES.getString("configuration.failed"), e);
    }
  }

  @JsonProperty(value = "baseUrl", required = true, index = 11)
  private String getActualBaseUrl() {
    return baseUrl != null ? baseUrl.toString() : "";
  }

  private void updateBaseUrlIfChanged(String oldValue, String newValue) {
    if (!newValue.equals(oldValue)) {
      baseUrl = URI.create(newValue);
      amphoraServiceUri =
          new AmphoraServiceUri(MessageFormat.format(AMPHORA_URL_FORMAT, baseUrl.toString()));
      castorServiceUri =
          new CastorServiceUri(MessageFormat.format(CASTOR_URL_FORMAT, baseUrl.toString()));
      ephemeralServiceUrl =
          URI.create(MessageFormat.format(EPHEMERAL_URL_FORMAT, baseUrl.toString()));
    }
  }

  public URI getBaseUrl() {
    return baseUrl;
  }

  @JsonProperty(value = "baseUrl", required = true, index = 11)
  private void setBaseUrl(String baseUrl) {
    this.baseUrl = URI.create(baseUrl);
  }

  public AmphoraServiceUri getAmphoraServiceUri() {
    return System.getenv(MessageFormat.format(AMPHORA_URL_ENV_KEY_FORMAT, providerNumber)) != null
        ? new AmphoraServiceUri(
            System.getenv(MessageFormat.format(AMPHORA_URL_ENV_KEY_FORMAT, providerNumber)))
        : amphoraServiceUri;
  }

  @JsonProperty(value = "amphoraServiceUrl", required = true, index = 12)
  private void setAmphoraServiceUri(String amphoraServiceAddress) {
    this.amphoraServiceUri = new AmphoraServiceUri(amphoraServiceAddress);
  }

  @JsonProperty(value = "amphoraServiceUrl", required = true, index = 12)
  private String getActualAmphoraServiceUri() {
    return amphoraServiceUri != null ? amphoraServiceUri.getServiceUri().toString() : "";
  }

  public CastorServiceUri getCastorServiceUri() {
    return System.getenv(MessageFormat.format(CASTOR_URL_ENV_KEY_FORMAT, providerNumber)) != null
        ? new CastorServiceUri(
            System.getenv(MessageFormat.format(CASTOR_URL_ENV_KEY_FORMAT, providerNumber)))
        : castorServiceUri;
  }

  @JsonProperty(value = "castorServiceUrl", required = true, index = 13)
  private void setCastorServiceUri(String castorServiceAddress) {
    this.castorServiceUri = new CastorServiceUri(castorServiceAddress);
  }

  @JsonProperty(value = "castorServiceUrl", required = true, index = 13)
  private String getActualCastorServiceUri() {
    return castorServiceUri != null ? castorServiceUri.getRestServiceUri().toString() : "";
  }

  public URI getEphemeralServiceUrl() {
    return System.getenv(MessageFormat.format(EPHEMERAL_URL_ENV_KEY_FORMAT, providerNumber)) != null
        ? URI.create(
            System.getenv(MessageFormat.format(EPHEMERAL_URL_ENV_KEY_FORMAT, providerNumber)))
        : ephemeralServiceUrl;
  }

  @JsonProperty(value = "ephemeralServiceUrl", required = true, index = 14)
  private String getActualEphemeralServiceUrl() {
    return ephemeralServiceUrl != null ? ephemeralServiceUrl.toString() : "";
  }

  @JsonProperty(value = "ephemeralServiceUrl", required = true, index = 14)
  private void setEphemeralServiceUrlUrl(String ephemeralServiceUrl) {
    this.ephemeralServiceUrl = URI.create(ephemeralServiceUrl);
  }

  public String getOAuth2ClientId() {
    return System.getenv(MessageFormat.format(OAUTH2_CLIENT_ID_ENV_KEY_FORMAT, providerNumber))
            != null
        ? System.getenv(MessageFormat.format(OAUTH2_CLIENT_ID_ENV_KEY_FORMAT, providerNumber))
        : oAuth2clientId;
  }

  @JsonProperty(value = "oauth2ClientId", required = true, index = 15)
  public void setOAuth2ClientId(String oAuth2ClientId) {
    this.oAuth2clientId = oAuth2ClientId;
  }

  @JsonProperty(value = "oauth2ClientId", required = true, index = 15)
  public String getActualOAuth2ClientId() {
    return oAuth2clientId != null ? oAuth2clientId : "";
  }

  public URI getOAuth2CallbackUrl() {
    return System.getenv(MessageFormat.format(OAUTH2_CALLBACK_URL_ENV_KEY_FORMAT, providerNumber))
            != null
        ? URI.create(
            System.getenv(MessageFormat.format(OAUTH2_CALLBACK_URL_ENV_KEY_FORMAT, providerNumber)))
        : oAuth2CallbackUrl;
  }

  @JsonProperty(value = "oauth2CallbackUrl", required = true, index = 16)
  private void setOAuth2CallbackUrl(String oAuth2CallbackUrl) {
    this.oAuth2CallbackUrl = URI.create(oAuth2CallbackUrl);
  }

  @JsonProperty(value = "oauth2CallbackUrl", required = true, index = 16)
  private String getActualOAuth2CallbackUrl() {
    return oAuth2CallbackUrl != null ? oAuth2CallbackUrl.toString() : "";
  }

  @JsonProperty(value = "oauth2AuthEndpointUri", required = true, index = 17)
  private String getActualOauth2AuthEndpointUri() {
    return oAuth2AuthEndpointUri != null ? oAuth2AuthEndpointUri.toString() : "";
  }

  public URI getOauth2AuthEndpointUri() {
    return System.getenv(MessageFormat.format(OAUTH2_AUTH_ENDPOINT_URI, providerNumber)) != null
        ? URI.create(System.getenv(MessageFormat.format(OAUTH2_AUTH_ENDPOINT_URI, providerNumber)))
        : oAuth2AuthEndpointUri;
  }

  @JsonProperty(value = "oauth2AuthEndpointUri", required = true, index = 17)
  public void setOauth2AuthEndpointUri(String oAuth2AuthEndpointUri) {
    this.oAuth2AuthEndpointUri = URI.create(oAuth2AuthEndpointUri);
  }

  @JsonProperty(value = "oauth2TokenEndpointUri", required = true, index = 18)
  private String getActualOauth2TokenEndpointUri() {
    return oAuth2TokenEndpointUri != null ? oAuth2TokenEndpointUri.toString() : "";
  }

  public URI getOauth2TokenEndpointUri() {
    return System.getenv(MessageFormat.format(OAUTH2_TOKEN_ENDPOINT_URI, providerNumber)) != null
        ? URI.create(System.getenv(MessageFormat.format(OAUTH2_TOKEN_ENDPOINT_URI, providerNumber)))
        : oAuth2TokenEndpointUri;
  }

  @JsonProperty(value = "oauth2TokenEndpointUri", required = true, index = 18)
  public void setOauth2TokenEndpointUri(String oAuth2TokenEndpointUri) {
    this.oAuth2TokenEndpointUri = URI.create(oAuth2TokenEndpointUri);
  }

  @Override
  public String toString() {
    return "VcpConfiguration{"
        + "providerNumber="
        + providerNumber
        + ", baseUrl="
        + baseUrl
        + ", amphoraServiceUrl="
        + amphoraServiceUri
        + ", castorServiceUrl="
        + castorServiceUri
        + ", ephemeralServiceUrl="
        + ephemeralServiceUrl
        + ", oauth2ClientId="
        + oAuth2clientId
        + ", oauth2AuthEndpointUri="
        + oAuth2AuthEndpointUri
        + ", oauth2TokenEndpointUri="
        + oAuth2TokenEndpointUri
        + ", oauth2CallbackUrl="
        + oAuth2CallbackUrl
        + '}';
  }
}
