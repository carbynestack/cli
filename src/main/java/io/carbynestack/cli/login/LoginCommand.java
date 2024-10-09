/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import io.carbynestack.cli.configuration.Configuration;
import io.carbynestack.cli.configuration.VcpConfiguration;
import io.carbynestack.cli.exceptions.CsCliConfigurationException;
import io.carbynestack.cli.util.OAuthUtil;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;

@Slf4j
@Parameters(
    resourceBundle = LoginCommand.LOGIN_MESSAGE_BUNDLE,
    commandDescriptionKey = "command-description",
    commandNames = {LoginCommand.COMMAND_NAME})
public class LoginCommand {

  public static final String LOGIN_MESSAGE_BUNDLE = "LoginMessageBundle";
  public static final String COMMAND_NAME = "login";
  /* Ephemeral port range used by many Linux kernels. */
  static final Range<Integer> DEFAULT_CALLBACK_PORTS = Range.between(32768, 61000);
  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LOGIN_MESSAGE_BUNDLE);
  private static final int AUTHENTICATION_CODE_TIMEOUT_MILLISECONDS = 120 * 1000;
  private final Range<Integer> callbackPortRange;
  private final BiFunction<URI, State, OAuth2AuthenticationCodeCallbackHttpServer>
      callbackServerProvider;

  @Getter
  @Parameter(names = "--help", descriptionKey = "option.help-description", help = true)
  private boolean help;

  public LoginCommand() {
    this(
        DEFAULT_CALLBACK_PORTS,
        (url, state) ->
            new OAuth2AuthenticationCodeCallbackHttpServer(
                url, state, AUTHENTICATION_CODE_TIMEOUT_MILLISECONDS));
  }

  LoginCommand(
      Range<Integer> callbackPortRange,
      BiFunction<URI, State, OAuth2AuthenticationCodeCallbackHttpServer> callbackServerProvider) {
    this.callbackPortRange = callbackPortRange;
    this.callbackServerProvider = callbackServerProvider;
  }

  private static TokenRequest createTokenRequest(
      VcpConfiguration vcpConfiguration,
      AuthorizationCode authzCode,
      URI callback,
      ClientID clientID) {
    AuthorizationGrant authzGrant = new AuthorizationCodeGrant(authzCode, callback);
    Scope scope = new Scope("offline", "openid");

    return new TokenRequest(
        vcpConfiguration.getOauth2TokenEndpointUri(), clientID, authzGrant, scope);
  }

  public void login() throws CsCliLoginException, CsCliConfigurationException {
    if (isHelp()) {
      System.out.println(StringEscapeUtils.unescapeJava(MESSAGES.getString("detailed-help")));
      return;
    }
    Configuration configuration = Configuration.getInstance();
    List<VcpToken> tokens = Lists.newArrayList();
    for (VcpConfiguration vcpConfig : configuration.getProviders()) {
      Either<? extends AuthenticationError, VcpToken> login =
          login(
              vcpConfig,
              callbackPortRange,
              configuration.isNoSslValidation(),
              configuration.getTrustedCertificates());
      tokens.add(login.getOrElseThrow(CsCliLoginException::new));
    }
    VcpTokenStore store = VcpTokenStore.builder().tokens(tokens).build();
    store.persist().getOrElseThrow(CsCliLoginException::new);
  }

  private Either<? extends AuthenticationError, VcpToken> login(
      VcpConfiguration vcpConfiguration,
      Range<Integer> callbackPortCandidates,
      boolean insecure,
      List<Path> trustedCertificates) {
    URI callbackUrl =
        Try.of(
                () ->
                    new URIBuilder(vcpConfiguration.getOAuth2CallbackUrl())
                        .setPort(callbackPortCandidates.getMinimum())
                        .build())
            .getOrElseThrow(t -> new IllegalStateException(t));

    log.debug("launching callback server on local address {}", callbackUrl);

    // used to compare against the state in the callback handler for CSRF protection
    // @see io.carbynestack.cli.login.OAuth2AuthenticationCodeCallbackHttpServer
    State state = new State();

    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        callbackServerProvider.apply(callbackUrl, state)) {

      ClientID clientID = new ClientID(vcpConfiguration.getOAuth2ClientId());
      URI callback = vcpConfiguration.getOAuth2CallbackUrl();
      Nonce nonce = new Nonce();

      AuthenticationRequest request =
          new AuthenticationRequest.Builder(
                  new ResponseType("code"), new Scope("openid", "offline"), clientID, callback)
              .endpointURI(vcpConfiguration.getOauth2AuthEndpointUri())
              .state(state)
              .nonce(nonce)
              .build();

      Option<? extends AuthenticationError> failed = BrowserLauncher.browse(request.toURI());
      if (failed.isDefined()) {
        return Either.left(failed.get());
      }

      return server
          .getAuthorizationCode()
          .map(
              code ->
                  Try.of(
                      () -> {
                        TokenResponse tokenResponse =
                            sendTokenRequest(
                                createTokenRequest(vcpConfiguration, code, callback, clientID),
                                insecure,
                                trustedCertificates);
                        if (tokenResponse instanceof TokenErrorResponse) {
                          throw new CsCliLoginException(
                              LoginCommandError.FAILED_TO_GET_ACCESS_TOKEN);
                        }

                        OIDCTokenResponse successResponse =
                            (OIDCTokenResponse) tokenResponse.toSuccessResponse();
                        return successResponse.getOIDCTokens();
                      }))
          .mapLeft(e -> (AuthenticationError) e)
          .flatMap(
              t ->
                  t.onFailure(e -> log.error("getting access token failed unexpectedly", e))
                      .toEither(LoginCommandError.FAILED_TO_GET_ACCESS_TOKEN))
          .map(token -> VcpToken.from(vcpConfiguration.getBaseUrl(), token));
    } catch (RuntimeException re) {
      if (re.getCause() instanceof BindException) {
        log.debug("opening local callback server on local address {} failed", callbackUrl);
        if (RangeUtils.getLength(callbackPortCandidates) <= 1) {
          return Either.left(LoginCommandError.PORT_RANGE_EXHAUSTION);
        }
        return login(
            vcpConfiguration,
            RangeUtils.consumeLower(callbackPortCandidates),
            insecure,
            trustedCertificates);
      } else {
        log.error("login failed for unexpected reason", re);
        return Either.left(LoginCommandError.UNEXPECTED);
      }
    }
  }

  protected TokenResponse sendTokenRequest(
      TokenRequest request, boolean insecure, List<Path> trustedCertificates)
      throws IOException, ParseException, NoSuchAlgorithmException, KeyStoreException,
          KeyManagementException, CertificateException {
    HTTPRequest httpRequest = request.toHTTPRequest();
    OAuthUtil.setSslContextForRequestWithConfiguration(httpRequest, insecure, trustedCertificates);
    return OIDCTokenResponseParser.parse(httpRequest.send());
  }

  public enum LoginCommandError implements AuthenticationError {

    /** Something went wrong when retrieving the access token */
    FAILED_TO_GET_ACCESS_TOKEN,

    /** No free port is available for launching the webserver for the callback */
    PORT_RANGE_EXHAUSTION,

    /** An unexpected error condition occurred while performing the login */
    UNEXPECTED
  }
}
