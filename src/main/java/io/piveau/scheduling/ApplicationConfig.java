package io.piveau.scheduling;

public final class ApplicationConfig {

    static final String ENV_APPLICATION_PORT = "PORT";
    static final int DEFAULT_APPLICATION_PORT = 8080;

    static final String ENV_PIVEAU_FAVICON_PATH = "PIVEAU_FAVICON_PATH";
    static final String DEFAULT_PIVEAU_FAVICON_PATH = "webroot/images/favicon.png";

    static final String ENV_PIVEAU_LOGO_PATH = "PIVEAU_LOGO_PATH";
    static final String DEFAULT_PIVEAU_LOGO_PATH = "webroot/images/logo.png";

    public static final String ENV_PIVEAU_CLUSTER_CONFIG = "PIVEAU_CLUSTER_CONFIG";
    public static final String ENV_PIVEAU_SHELL_CONFIG = "PIVEAU_SHELL_CONFIG";

}
