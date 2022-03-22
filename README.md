# Carbyne Stack Command Line Interface

[![codecov](https://codecov.io/gh/carbynestack/cli/branch/master/graph/badge.svg?token=ja4W6WLOHO)](https://codecov.io/gh/carbynestack/cli)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/67fd8c2ab94f4756a0d5cfc326ac0567)](https://www.codacy.com?utm_source=github.com&utm_medium=referral&utm_content=carbynestack/cli&utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/carbynestack/cli/badge.svg)](https://snyk.io/test/github/carbynestack/cli)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit&logoColor=white)](https://github.com/pre-commit/pre-commit)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

This is a CLI tool to communicate with the Carbyne Stack services.

> **DISCLAIMER**: The Carbyne Stack CLI is *alpha* software. The software is not
> ready for production use. It has neither been developed nor tested for a
> specific use case.

> **IMPORTANT**: The Carbyne Stack community has started to implement the CLI
> from scratch (see the [cli-ng](https://github.com/carbynestack/cli-ng)
> repository). As we are focusing on achieving feature parity as soon as
> possible, this version of the CLI is considered legacy and will no longer be
> maintained.

## Configuration

Prior to the first use of the Carbyne Stack CLI it is required to run the
_configure_ command once:

```bash
cs configure
```

This command will guide you through a step-by-step configuration to match your
Virtual Cloud setup.

While it is required to run the configuration at least once prior to the first
command execution, it is possible to overload some settings using environment
variables.

The following environment variables can be used to adapt your configuration
without overwriting the default config:

| Variable                   | Description                                                                   |
| -------------------------- | ----------------------------------------------------------------------------- |
| CS_PRIME                   | Modulus N as used by the MPC backend                                          |
| CS_R                       | Auxiliary modulus R as used by the MPC backend                                |
| CS_R_INV                   | Multiplicative inverse for the auxiliary modulus R as used by the MPC backend |
| CS_NO_SSL_VALIDATION       | Disable SSL certificate validation                                            |
| CS_VCP\_{n}\_BASE_URL      | Base URL for the provider with ID "{n}"                                       |
| CS_VCP\_{n}\_AMPHORA_URL   | Amphora Service URL for the provider with ID "{n}"                            |
| CS_VCP\_{n}\_CASTOR_URL    | Castor Service URL for the provider with ID "{n}"                             |
| CS_VCP\_{n}\_EPHEMERAL_URL | Ephemeral Service URL for the provider with ID "{n}"                          |

> **NOTE:** Please be aware, that you can only adapt the configuration for
> providers defined in your default config. It is not possible to add additional
> or remove existing providers. These variables are used during the
> configuration process only.

## Usage

The general usage of the CLI is as follows:

```text
Usage: cs [options] [command] [subcommand] [subcommand options] [parameter]
  Options:
    --config-file
      Configuration file used instead of reading the default configuration
      from "~/.specs/config".
    --debug
      Set log level to debug.
    --help
      Displays this help message.
  Commands:
    configure      Set the SPECS CLI default configuration
      Usage: configure [options]
        Options:
          --help
            Show help message on how to use and adapt the configuration.
          show
            Prints the CLI configuration that is currently in use.

    amphora      Create and retrieve secrets from Amphora service(s)
      Subcommands:
        create-secret      Creates a new Amphora secret with the given secret data.
          Usage: create-secret [options] SECRET [SECRET... (type: Long)]

                Although it is mandatory to define at least one secret to be
                shared within the new secret, secrets might also be passed on
                StdIn (separated by new line) if omitted on command.

            Options:
              -i, --secret-id
                UUID which will be used as unique identifier to store the secret.
                Default: <Random UUID>
              -t, --tag
                A Tag that will be added to the given secret.
                Format: <KEY>=<VALUE>

        get-secret      Retrieve an secret from Amphora
          Usage: get-secret [options] SECRET_ID
            Options:
              -f, --tagfilter
                Filter secrets based on their tags and the criteria provided.
                When multiple filters are defined, they are joined via AND.
                    Can be in one of the following formats:
                        - key:value EQUAL Match
                        - key>value GREATER THAN (Assumes tag value is a numeric value)
                        - key<value LESS THAN (Assumes tag value is a numeric value)
              -s, --sortBy
                Sort output by given tag key.
                    Format: <KEY>:[ASC|DESC]
                    Examples:
                        type:ASC
                        creation-date:DESC
              -l, --list-ids-only
                Output secret IDs only

        get-secrets      Lists all Secrets stored on the given Amphora service(s).
          Usage: get-secrets [options]

        create-tag      Adds a new Tag to the given Amphora secret
          Usage: create-tag [options] TAG

                Tag format: <KEY>=<VALUE>

            Options:
            * -i, --secret-id
                ID of the secret for which the tag will be created

        get-tags      Retrieve all tags of an secret from Amphora
          Usage: get-tags [options] SECRET_ID

        overwrite-tags      Replace all tags of an secret with new tags
          Usage: overwrite-tags [options] TAG [TAG...]

                Tag format: <KEY>=<VALUE>

            Options:
            * -i, --secret-id
                ID of the related Amphora secret

        get-tag      Retrieve a tag of an secret from Amphora
          Usage: get-tag [options] TAG_KEY
            Options:
            * -i, --secret-id
                ID of the related Amphora secret

        update-tag      Updates the value of an secret's tag. If a tag with the
                        same key is not already present at the secret, it
                        will be created.
          Usage: update-tag [options] TAG

                Tag format: <KEY>=<VALUE>

            Options:
            * -i, --secret-id
                ID of the related Amphora secret

        delete-tag      Delete a tag from an secret from Amphora
          Usage: delete-tag [options] TAG_KEY
            Options:
            * -i, --secret-id
                ID of the Amphora secret

        delete-secrets      Delete an secret from Amphora
          Usage: delete-secrets [options] SECRET_ID [SECRET_ID...]

                Although it is mandatory to define at least one Secret-ID to be
                deleted, IDs might also be passed on StdIn (separated by new line)
                 if omitted on command.

    castor      Upload to and download from Castor Service
      Subcommands:
        upload-tuple      Upload a tuple file to Castor Service
          Usage: upload-tuple [options] PROVIDER_ID

                Provider-ID as given by the configuration

            Options:
              -i, --chunk-id
                Unique identifier as to be used by Castor
                Default: <Random UUID>
            * -f, --tuple-file
                Tuple file path
            * -t, --tuple-type
                Tuple type, e.g., MULTIPLICATION_TRIPLE_GFP

        get-telemetry      Download telemetry data
          Usage: get-telemetry [options] PROVIDER_ID

                Provider-ID as given by the configuration

            Options:
              -i, --interval
                Interval in seconds to get the telemetry data for

        activate-chunk      Upload a tuple file to Castor Service
          Usage: activate-chunk [options] PROVIDER_ID

                Provider-ID as given by the configuration

            Options:
            * -i, --chunk-id
                Unique identifier of the tuple chunk

    ephemeral      Execute functions using Ephemeral
      Subcommands
        execute      Invokes an Ephemeral function with the given inputs secrets.
          Usage: execute [options] APPLICATION_NAME
            Options:
              -i, --input
                UUID of an Amphora Secret used as secret input for the function
                execution.
                This option can be defined multiple times in order to to use
                multiple secrets as input for the execution.
              -t, --timeout
                Maximum time allowed for the request in seconds.
                Default: 10
```

## License

The Carbyne Stack *Command Line Interface* is open-sourced under the Apache
License 2.0. See the [LICENSE](LICENSE) file for details.

### 3rd Party Licenses

For information on how license obligations for 3rd party OSS dependencies are
fulfilled see the [README](https://github.com/carbynestack/carbynestack) file of
the Carbyne Stack repository.

## Contributing

Please see the Carbyne Stack
[Contributor's Guide](https://github.com/carbynestack/carbynestack/blob/master/CONTRIBUTING.md).
