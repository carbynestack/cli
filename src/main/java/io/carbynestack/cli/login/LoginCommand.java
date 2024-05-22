/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/cli.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.cli.login;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
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
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.net.BindException;
import java.net.URI;
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

  public enum LoginCommandError implements AuthenticationError {

    /** Something went wrong when retrieving the access token */
    FAILED_TO_GET_ACCESS_TOKEN,

    /** No free port is available for launching the webserver for the callback */
    PORT_RANGE_EXHAUSTION,

    /** An unexpected error condition occurred while performing the login */
    UNEXPECTED
  }

  public static final String LOGIN_MESSAGE_BUNDLE = "LoginMessageBundle";
  public static final String COMMAND_NAME = "login";

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LOGIN_MESSAGE_BUNDLE);
  private static final String[] SCOPES =
      new String[] {"openid", "email", "profile", "offline_access"};

  /* Ephemeral port range used by many Linux kernels. */
  static final Range<Integer> DEFAULT_CALLBACK_PORTS = Range.between(32768, 61000);
  static final String TENANT_ID = "0ae51e19-07c8-4e4b-bb6d-648ee58410f4";
  private static final int AUTHENTICATION_CODE_TIMEOUT_MILLISECONDS = 120 * 1000;

  @Getter
  @Parameter(names = "--help", descriptionKey = "option.help-description", help = true)
  private boolean help;

  private final Range<Integer> callbackPortRange;
  private final BiFunction<VcpConfiguration, URI, OAuth20Service> oAuth20ServiceProvider;
  private final BiFunction<URI, State, OAuth2AuthenticationCodeCallbackHttpServer>
      callbackServerProvider;

  public LoginCommand() {
    this(
        DEFAULT_CALLBACK_PORTS,
        (vcpConfig, callbackUrl) ->
            new ServiceBuilder(vcpConfig.getOAuth2ClientId())
                .defaultScope(String.join(" ", SCOPES))
                .callback(callbackUrl.toString())
                .build(MicrosoftAzureActiveDirectory20Api.custom(TENANT_ID)),
        (url, state) ->
            new OAuth2AuthenticationCodeCallbackHttpServer(
                url, state, AUTHENTICATION_CODE_TIMEOUT_MILLISECONDS));
  }

  LoginCommand(
      Range<Integer> callbackPortRange,
      BiFunction<VcpConfiguration, URI, OAuth20Service> oAuth20ServiceProvider,
      BiFunction<URI, State, OAuth2AuthenticationCodeCallbackHttpServer> callbackServerProvider) {
    this.callbackPortRange = callbackPortRange;
    this.oAuth20ServiceProvider = oAuth20ServiceProvider;
    this.callbackServerProvider = callbackServerProvider;
  }

  public void login() throws CsCliLoginException, CsCliConfigurationException {
    if (isHelp()) {
      System.out.println(StringEscapeUtils.unescapeJava(MESSAGES.getString("detailed-help")));
      return;
    }
    Configuration configuration = Configuration.getInstance();
    List<VcpToken> tokens = Lists.newArrayList();
    for (VcpConfiguration vcpConfig : configuration.getProviders()) {
      Either<? extends AuthenticationError, VcpToken> login = login(vcpConfig, callbackPortRange);
      tokens.add(login.getOrElseThrow(CsCliLoginException::new));
    }
    VcpTokenStore store = VcpTokenStore.builder().tokens(tokens).build();
    store.persist().getOrElseThrow(CsCliLoginException::new);
  }

  private Either<? extends AuthenticationError, VcpToken> login(
      VcpConfiguration vcpConfiguration, Range<Integer> callbackPortCandidates) {
    URI callbackUrl =
        Try.of(
                () ->
                    new URIBuilder(vcpConfiguration.getOAuth2CallbackUrl())
                        .setPort(callbackPortCandidates.getMinimum())
                        .build())
            .getOrElseThrow(t -> new IllegalStateException(t));

    log.debug("launching callback server on local address {}", callbackUrl);

    State state = new State(); // used to compare against the state in the callback for CSRF
    try (OAuth2AuthenticationCodeCallbackHttpServer server =
        callbackServerProvider.apply(callbackUrl, state)) {

      ClientID clientID = new ClientID(vcpConfiguration.getOAuth2ClientId());
      URI callback = vcpConfiguration.getOAuth2CallbackUrl();
      Nonce nonce = new Nonce(); // used to compare against the nonce in the callback for CSRF

      // Compose the OpenID authentication request (for the code flow)
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
                        URI tokenEndpoint = vcpConfiguration.getOauth2TokenEndpointUri();
                        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

                        Scope authScope = new Scope();
                        authScope.add("offline");
                        authScope.add("openid");

                        TokenRequest tokenRequest =
                            new TokenRequest(tokenEndpoint, clientID, codeGrant, authScope);
                        HTTPRequest toHTTPRequest = tokenRequest.toHTTPRequest();
                        TokenResponse tokenResponse =
                            OIDCTokenResponseParser.parse(toHTTPRequest.send());
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
        return login(vcpConfiguration, RangeUtils.consumeLower(callbackPortCandidates));
      } else {
        log.error("login failed for unexpected reason", re);
        return Either.left(LoginCommandError.UNEXPECTED);
      }
    }
  }
}
