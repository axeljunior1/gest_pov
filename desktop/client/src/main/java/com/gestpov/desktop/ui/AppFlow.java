package com.gestpov.desktop.ui;

import com.gestpov.desktop.config.ClientConfig;
import com.gestpov.desktop.config.ClientConfigStore;
import com.gestpov.desktop.discovery.DiscoveredServer;
import com.gestpov.desktop.discovery.DiscoveryService;
import com.gestpov.desktop.discovery.UdpDiscoveryClient;
import com.gestpov.desktop.net.ApiClient;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.AuthClient;
import com.gestpov.desktop.net.AuthSession;
import com.gestpov.desktop.net.SettingsClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.license.LicenseView;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import com.gestpov.desktop.version.CompatibilityStatus;
import com.gestpov.desktop.version.VersionCompatibility;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Duration;
import java.util.List;

/**
 * Flux Phase 3 conservé : discovery → login → MainWindow.
 */
public final class AppFlow {

    private final Stage stage;
    private final ClientConfigStore store;
    private ClientConfig config;
    private ApiClient api;
    private SessionContext session;
    private AuthClient auth;

    public AppFlow(Stage stage) {
        this.stage = stage;
        this.store = ClientConfigStore.userDefault();
        this.config = store.load();
    }

    public void start() {
        stage.setTitle("Gest POV Desktop");
        showDiscovery();
        stage.show();
    }

    public void showDiscovery() {
        Label status = new Label("Recherche du serveur Gest POV…");
        UiTheme.apply(stage, box(title("Gest POV Desktop"), status), 560, 420);
        FxAsync.run(() -> {
            DiscoveryService discovery = new DiscoveryService(
                    new UdpDiscoveryClient(Duration.ofMillis(1500)),
                    Duration.ofMillis(config.timeoutMs()),
                    config.clientVersion().isBlank() ? ClientConfig.CURRENT_VERSION : config.clientVersion()
            );
            return discovery.findServers(config);
        }, this::showServerChoice, error -> showManualConnect("Aucun serveur Gest POV trouvé sur le réseau."));
    }

    private void showServerChoice(List<DiscoveredServer> found) {
        if (found.isEmpty()) {
            showManualConnect("Aucun serveur Gest POV trouvé sur le réseau.");
            return;
        }
        if (found.size() == 1) {
            connectTo(found.get(0));
            return;
        }
        Label heading = new Label("Plusieurs serveurs détectés");
        ComboBox<DiscoveredServer> combo = new ComboBox<>();
        combo.getItems().addAll(found);
        combo.setValue(preferRemembered(found));
        combo.setPrefWidth(420);
        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(DiscoveredServer server) {
                return server == null ? "" : server.label();
            }

            @Override
            public DiscoveredServer fromString(String string) {
                return null;
            }
        });
        Button use = new Button("Utiliser ce serveur");
        use.getStyleClass().add("button-primary");
        use.setOnAction(e -> {
            if (combo.getValue() != null) {
                connectTo(combo.getValue());
            }
        });
        UiTheme.apply(stage, box(title("Gest POV Desktop"), heading, combo, use), 560, 420);
    }

    private DiscoveredServer preferRemembered(List<DiscoveredServer> found) {
        if (config.serverId() != null && !config.serverId().isBlank()) {
            return found.stream()
                    .filter(s -> config.serverId().equals(s.serverId()))
                    .findFirst()
                    .orElse(found.get(0));
        }
        return found.get(0);
    }

    private void connectTo(DiscoveredServer server) {
        if (!server.isGestPov()) {
            showManualConnect("Serveur rejeté : ce n'est pas Gest POV.");
            return;
        }
        if (!VersionCompatibility.canConnect(server.compatibility())) {
            String msg = server.compatibility() == CompatibilityStatus.UPDATE_REQUIRED
                    ? "Mettez à jour Gest POV Desktop (serveur plus récent)."
                    : "Le serveur est trop ancien. Mettez-le à jour.";
            showManualConnect(msg);
            return;
        }
        this.api = new ApiClient(server.host(), server.port(), Duration.ofMillis(config.timeoutMs()));
        this.session = new SessionContext(api);
        this.session.bindServer(server.serverId(), server.serverName(), server.version());
        this.session.onSessionExpired(() -> Platform.runLater(() ->
                showLogin(server, "Votre session a expiré. Veuillez vous reconnecter.")));
        this.auth = new AuthClient(api);
        try {
            config = config.withServer(server.serverId(), server.host(), server.port(), server.serverName());
            store.save(config);
        } catch (Exception ignored) {
            // config locale optionnelle
        }
        showLogin(server, null);
    }

    private void showManualConnect(String message) {
        Label info = new Label(message);
        info.setWrapText(true);
        TextField host = new TextField(config.hasRememberedServer() ? config.host() : "127.0.0.1");
        TextField port = new TextField(String.valueOf(config.port() > 0 ? config.port() : 8080));
        Button retry = new Button("Réessayer la découverte");
        retry.getStyleClass().add("button-secondary");
        retry.setOnAction(e -> showDiscovery());
        Button connect = new Button("Valider ce serveur (HTTP)");
        connect.getStyleClass().add("button-primary");
        connect.setOnAction(e -> {
            try {
                int p = Integer.parseInt(port.getText().trim());
                DiscoveryService discovery = new DiscoveryService(
                        new UdpDiscoveryClient(Duration.ofMillis(200)),
                        Duration.ofMillis(config.timeoutMs()),
                        ClientConfig.CURRENT_VERSION
                );
                DiscoveredServer server = discovery.validateHttp(host.getText().trim(), p);
                connectTo(server);
            } catch (NumberFormatException ex) {
                info.setText("Port invalide.");
            } catch (ApiException ex) {
                info.setText(ApiException.userMessage(ex));
            }
        });
        UiTheme.apply(stage, box(
                title("Connexion serveur"),
                info,
                labeled("Hôte", host),
                labeled("Port API", port),
                connect,
                retry
        ), 560, 420);
    }

    private void showLogin(DiscoveredServer server, String preset) {
        Label status = new Label(preset != null ? preset : (
                "Serveur : " + server.label()
                        + "\nVersion serveur " + server.version()
                        + " — " + server.compatibility()));
        status.setWrapText(true);
        status.setMaxWidth(480);
        status.getStyleClass().add("status-message");
        if (preset != null && !preset.isBlank()) {
            status.getStyleClass().add("status-message-error");
        }

        javafx.scene.control.ScrollPane statusScroll = new javafx.scene.control.ScrollPane(status);
        statusScroll.setFitToWidth(true);
        statusScroll.setMaxHeight(140);
        statusScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        statusScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        String remembered = config.lastLoginEmail() == null ? "" : config.lastLoginEmail().trim();
        TextField email = new TextField(remembered);
        email.setPromptText("email@exemple.local");
        PasswordField password = new PasswordField();
        Button login = new Button("Connexion");
        login.getStyleClass().add("button-primary");
        login.setOnAction(e -> doLogin(server, email.getText().trim(), password.getText(), status, login));
        password.setOnAction(e -> login.fire());
        Button back = new Button("Changer de serveur");
        back.getStyleClass().add("button-ghost");
        back.setOnAction(e -> showDiscovery());

        UiTheme.apply(stage, box(
                title("Connexion Gest POV"),
                statusScroll,
                labeled("Email", email),
                labeled("Mot de passe", password),
                login,
                back
        ), 560, 480);
        Platform.runLater(() -> {
            if (email.getText() == null || email.getText().isBlank()) {
                email.requestFocus();
            } else {
                password.requestFocus();
            }
        });
    }

    private void doLogin(DiscoveredServer server, String email, String password, Label status, Button login) {
        if (email == null || email.isBlank()) {
            status.setText("Saisissez votre email.");
            return;
        }
        login.setDisable(true);
        status.setText("Connexion…");
        FxAsync.run(() -> {
            AuthSession loggedIn = auth.login(email, password);
            try {
                return new LoginOutcome(auth.me(), null);
            } catch (ApiException api) {
                if (api.isLicenseRequired()) {
                    return new LoginOutcome(loggedIn, api);
                }
                throw api;
            }
        }, outcome -> {
            try {
                config = config.withLastLoginEmail(email);
                store.save(config);
            } catch (Exception ignored) {
                // config locale optionnelle
            }
            if (outcome.licenseError() != null) {
                showLicenseActivation(server, outcome.session(), outcome.licenseError());
            } else {
                showHome(server, outcome.session());
            }
        }, error -> {
            login.setDisable(false);
            if (error instanceof ApiException api) {
                status.setText(ApiException.loginMessage(api));
                if (!status.getStyleClass().contains("status-message-error")) {
                    status.getStyleClass().add("status-message-error");
                }
            } else {
                status.setText("Connexion au serveur impossible. Vérifiez que le serveur Gest POV est démarré.");
                if (!status.getStyleClass().contains("status-message-error")) {
                    status.getStyleClass().add("status-message-error");
                }
            }
        });
    }

    private void showLicenseActivation(DiscoveredServer server, AuthSession me, ApiException licenseError) {
        session.setUser(me);
        stage.setMinWidth(720);
        stage.setMinHeight(560);
        Label heading = title("Licence requise");
        Label explain = new Label(
                "Le serveur exige une licence. Copiez le server.id ci-dessous, générez le fichier .lic, puis importez-le.");
        explain.getStyleClass().add("page-sub");
        explain.setWrapText(true);

        String presetId = licenseError.installationId();
        if (presetId == null || presetId.isBlank()) {
            presetId = server.serverId();
        }

        LicenseView licenseView = new LicenseView(session, () -> {
            FxAsync.run(auth::me, refreshed -> showHome(server, refreshed), err -> {
                if (err instanceof ApiException api && api.isLicenseRequired()) {
                    // reste sur cet ecran
                } else if (err instanceof ApiException api) {
                    showLogin(server, ApiException.loginMessage(api));
                }
            });
        });

        Button back = new Button("Retour connexion");
        back.getStyleClass().add("button-ghost");
        back.setOnAction(e -> {
            session.clear();
            showLogin(server, null);
        });

        VBox root = box(heading, explain, licenseView, back);
        if (presetId != null && !presetId.isBlank()) {
            Label idLine = new Label("server.id : " + presetId);
            idLine.getStyleClass().add("license-hero-id");
            idLine.setWrapText(true);
            Button copy = new Button("Copier server.id");
            copy.getStyleClass().add("button-primary");
            String idCopy = presetId;
            copy.setOnAction(e -> {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(idCopy);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            });
            root.getChildren().add(2, new VBox(8, idLine, copy));
        }

        UiTheme.apply(stage, root, 780, 640);
        stage.setTitle("Gest POV — Activation licence");
    }

    private void showHome(DiscoveredServer server, AuthSession me) {
        session.setUser(me);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        // Charge la devise configuree (endpoint public, lisible par tout role) avant d'afficher le moindre montant.
        FxAsync.run(() -> new SettingsClient(session.api()).getPublicCurrency(),
                currency -> {
                    ProductLabels.setCurrency(currency);
                    openMainWindow(server);
                },
                ignored -> openMainWindow(server));
    }

    private void openMainWindow(DiscoveredServer server) {
        MainWindow main = new MainWindow(session, this::logout);
        UiTheme.apply(stage, main, 1100, 720);
        stage.setTitle("Gest POV — " + (server.serverName() == null ? "Desktop" : server.serverName()));
    }

    private record LoginOutcome(AuthSession session, ApiException licenseError) {
    }

    private void logout() {
        if (session != null) {
            session.clear();
        }
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setTitle("Gest POV Desktop");
        showDiscovery();
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    private static VBox box(javafx.scene.Node... nodes) {
        VBox root = new VBox(12, nodes);
        root.setPadding(new Insets(20));
        return root;
    }

    private static VBox labeled(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("brand-sub");
        return new VBox(4, l, field);
    }
}
