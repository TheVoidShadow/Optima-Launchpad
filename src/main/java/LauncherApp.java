import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LauncherApp extends Application {

    private List<AppItem> appList = new ArrayList<>();
    private final FlowPane gridPane = new FlowPane();
    private boolean isAdmin = false;
    private Button adminBtn;
    private static final String CONFIG_FILE = "config.properties";
    private static final String APPS_FILE = "apps.dat";

    public static class AppItem implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        String target;
        boolean isWeb;
        String iconUrl;

        public AppItem(String name, String target, boolean isWeb, String iconUrl) {
            this.name = name;
            this.target = target;
            this.isWeb = isWeb;
            this.iconUrl = iconUrl;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        if (!configExists()) {
            showAccountCreationModal();
        }

        // Load saved apps or seed defaults if no file exists
        loadAppsFromFile();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #121212;");

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: #1E1E1E; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        Label title = new Label("Workspace Launcher");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        adminBtn = new Button("Admin Login");
        styleButton(adminBtn, "#007ACC");
        adminBtn.setOnAction(e -> handleAdminToggle());

        topBar.getChildren().addAll(title, adminBtn);
        root.setTop(topBar);

        gridPane.setPadding(new Insets(30));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #121212; -fx-background-color: #121212; -fx-border-color: transparent;");

        root.setCenter(scrollPane);

        renderGrid();

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("App Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void saveAppsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(APPS_FILE))) {
            oos.writeObject(appList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAppsFromFile() {
        File file = new File(APPS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                appList = (List<AppItem>) ois.readObject();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Default seed apps if no saved file exists
        appList.add(new AppItem("Gmail", "https://mail.google.com", true, "https://www.google.com/s2/favicons?domain=mail.google.com&sz=128"));
        appList.add(new AppItem("Google", "https://google.com", true, "https://www.google.com/s2/favicons?domain=google.com&sz=128"));
        appList.add(new AppItem("GitHub", "https://github.com", true, "https://www.google.com/s2/favicons?domain=github.com&sz=128"));
        saveAppsToFile();
    }

    private boolean configExists() {
        return new File(CONFIG_FILE).exists();
    }

    private void saveCredentials(String username, String password) {
        Properties props = new Properties();
        props.setProperty("admin.user", username);
        props.setProperty("admin.pass", password);
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Admin Credentials");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean validateCredentials(String username, String password) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            return username.equals(props.getProperty("admin.user")) && password.equals(props.getProperty("admin.pass"));
        } catch (IOException ex) {
            return false;
        }
    }

    private void showAccountCreationModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Initial Setup - Create Admin Account");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1E1E1E;");

        Label header = new Label("Welcome! Set up your Admin Account");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Choose Admin Username");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Choose Admin Password");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm Admin Password");

        Button createBtn = new Button("Create Account");
        styleButton(createBtn, "#28A745");

        createBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                showAlert("Username and password cannot be empty!");
            } else if (!pass.equals(confirmPassField.getText())) {
                showAlert("Passwords do not match!");
            } else {
                saveCredentials(user, pass);
                modal.close();
            }
        });

        layout.getChildren().addAll(header, userField, passField, confirmPassField, createBtn);
        modal.setScene(new Scene(layout, 380, 300));
        modal.setOnCloseRequest(e -> { if (!configExists()) System.exit(0); });
        modal.showAndWait();
    }

    private void renderGrid() {
        gridPane.getChildren().clear();

        for (int i = 0; i < appList.size(); i++) {
            AppItem item = appList.get(i);
            int index = i;

            StackPane card = new StackPane();
            card.setPrefSize(110, 110);
            card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

            VBox content = new VBox(8);
            content.setAlignment(Pos.CENTER);

            ImageView iconView = new ImageView();
            iconView.setFitWidth(64);
            iconView.setFitHeight(64);
            iconView.setPreserveRatio(true);

            String iconPath = getIconUrlForItem(item);
            try {
                iconView.setImage(new Image(iconPath, true));
            } catch (Exception ex) {
                iconView.setImage(new Image("https://via.placeholder.com/64/007ACC/FFFFFF?text=APP"));
            }

            Label nameLabel = new Label(item.name);
            nameLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 13px; -fx-font-weight: 600;");
            nameLabel.setWrapText(true);

            content.getChildren().addAll(iconView, nameLabel);
            card.getChildren().add(content);

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), card);
            scaleUp.setToX(1.15);
            scaleUp.setToY(1.15);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), card);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            card.setOnMouseEntered(e -> scaleUp.playFromStart());
            card.setOnMouseExited(e -> scaleDown.playFromStart());

            card.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    launchTarget(item);
                }
            });

            if (isAdmin) {
                HBox actionButtons = new HBox(5);
                actionButtons.setAlignment(Pos.TOP_RIGHT);
                StackPane.setAlignment(actionButtons, Pos.TOP_RIGHT);

                Button editBtn = new Button("✏️");
                editBtn.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 10px;");
                editBtn.setOnAction(e -> {
                    e.consume();
                    showEditAppModal(item);
                });

                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: #FF4D4D; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 10px;");
                removeBtn.setOnAction(e -> {
                    e.consume();
                    appList.remove(index);
                    saveAppsToFile();
                    renderGrid();
                });

                actionButtons.getChildren().addAll(editBtn, removeBtn);
                card.getChildren().add(actionButtons);
            }

            gridPane.getChildren().add(card);
        }

        if (isAdmin) {
            StackPane addCard = new StackPane();
            addCard.setPrefSize(110, 110);
            addCard.setStyle("-fx-background-color: transparent; -fx-border-color: #007ACC; -fx-border-style: dashed; -fx-border-radius: 12px; -fx-cursor: hand;");

            Label addLabel = new Label("+ Add App");
            addLabel.setStyle("-fx-text-fill: #007ACC; -fx-font-weight: bold;");
            addCard.getChildren().add(addLabel);

            addCard.setOnMouseClicked(e -> showAddAppModal());
            gridPane.getChildren().add(addCard);
        }
    }

    private String getIconUrlForItem(AppItem item) {
        if (item.iconUrl != null && !item.iconUrl.isEmpty()) {
            return item.iconUrl;
        }
        if (item.isWeb) {
            try {
                URI uri = new URI(item.target.startsWith("http") ? item.target : "https://" + item.target);
                String domain = uri.getHost();
                return "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128";
            } catch (Exception ex) {
                return "https://www.google.com/s2/favicons?domain=google.com&sz=128";
            }
        }
        return "https://via.placeholder.com/64/007ACC/FFFFFF?text=APP";
    }

    private void handleAdminToggle() {
        if (isAdmin) {
            isAdmin = false;
            adminBtn.setText("Admin Login");
            styleButton(adminBtn, "#007ACC");
            renderGrid();
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Admin Authentication");
        dialog.setHeaderText("Enter Admin Credentials");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField user = new TextField();
        PasswordField pass = new PasswordField();

        grid.add(new Label("User:"), 0, 0);
        grid.add(user, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(pass, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> dialogButton == loginButtonType && validateCredentials(user.getText(), pass.getText()));

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                isAdmin = true;
                adminBtn.setText("Lock Admin Mode");
                styleButton(adminBtn, "#28A745");
                renderGrid();
            } else {
                showAlert("Invalid Credentials!");
            }
        });
    }

    private void showAddAppModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Add App or Web Target");

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1E1E1E;");

        TextField nameField = new TextField();
        nameField.setPromptText("App Name");

        TextField targetField = new TextField();
        targetField.setPromptText("Exec Path or Web URL");

        TextField iconField = new TextField();
        iconField.setPromptText("Custom Icon URL (Optional)");

        CheckBox webCheck = new CheckBox("Is Web Link?");
        webCheck.setStyle("-fx-text-fill: white;");

        Button saveBtn = new Button("Add to Grid");
        styleButton(saveBtn, "#007ACC");

        saveBtn.setOnAction(e -> {
            if (!nameField.getText().isEmpty() && !targetField.getText().isEmpty()) {
                appList.add(new AppItem(nameField.getText(), targetField.getText(), webCheck.isSelected(), iconField.getText()));
                saveAppsToFile();
                renderGrid();
                modal.close();
            }
        });

        layout.getChildren().addAll(nameField, targetField, iconField, webCheck, saveBtn);
        modal.setScene(new Scene(layout, 350, 280));
        modal.show();
    }

    private void showEditAppModal(AppItem item) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Edit App Target");

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1E1E1E;");

        TextField nameField = new TextField(item.name);
        TextField targetField = new TextField(item.target);
        TextField iconField = new TextField(item.iconUrl != null ? item.iconUrl : "");
        iconField.setPromptText("Custom Icon URL (Optional)");

        CheckBox webCheck = new CheckBox("Is Web Link?");
        webCheck.setSelected(item.isWeb);
        webCheck.setStyle("-fx-text-fill: white;");

        Button saveBtn = new Button("Save Changes");
        styleButton(saveBtn, "#28A745");

        saveBtn.setOnAction(e -> {
            if (!nameField.getText().isEmpty() && !targetField.getText().isEmpty()) {
                item.name = nameField.getText();
                item.target = targetField.getText();
                item.isWeb = webCheck.isSelected();
                item.iconUrl = iconField.getText();
                saveAppsToFile();
                renderGrid();
                modal.close();
            }
        });

        layout.getChildren().addAll(nameField, targetField, iconField, webCheck, saveBtn);
        modal.setScene(new Scene(layout, 350, 280));
        modal.show();
    }

    private void launchTarget(AppItem item) {
        new Thread(() -> {
            try {
                if (item.isWeb) {
                    String url = item.target.startsWith("http") ? item.target : "https://" + item.target;
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec(new String[]{"open", url});
                    } else {
                        Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                    }
                } else {
                    new ProcessBuilder(item.target).start();
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Failed to launch: " + ex.getMessage()));
            }
        }).start();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
