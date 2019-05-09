package com.dgs.dapc.itemDB.javafx.splash;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Splash{
    private Stage stage;
    public ProgressBar progressIndicator;
    public Label titleLabel;
    public Label subtitleLabel;
    public Label processLabel;
    public Button cancelButton;
    private SplashTask task;

    public static <T> Splash getAwaitSplash(SplashTask<T> task) throws IOException {
        return getAwaitSplash(task,null);
    }

    public static <T> Splash getAwaitSplash(SplashTask<T> task, Stage stage) throws IOException {
        Splash instance = getInstance(stage);
        instance.task = task;
        instance.titleProperty().bind(task.titleProperty());

        instance.subtitleProperty().bind(new StringBinding() {
            {
                bind(task.stateProperty());
            }

            @Override
            protected String computeValue() {
                return "Task state: " + task.getState();
            }
        });
        instance.messageProperty().bind(task.messageProperty());
        instance.progressProperty().bind(task.progressProperty());

        instance.cancelButton.setDisable(task.getNotCancellable());
        task.isNotCancellableProperty().addListener((observable, oldValue, newValue) ->
                instance.cancelButton.setDisable(newValue == null ? true : newValue));//INSTANT CHANGE

        instance.cancelButton.setOnAction(event -> task.cancel());
        instance.cancelButton.setVisible(true);

        task.stateProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
                case CANCELLED:
                case SUCCEEDED:
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(instance::hide);
                    }).start();
            }
        });

        task.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue!=null) newValue.printStackTrace();//todo refactor to log
        });

        return instance;
    }

    public static Splash getInstance(Stage parentStage) throws IOException{
        FXMLLoader loader = new FXMLLoader(Splash.class.getResource("SplashView.fxml"));
        Parent root= loader.load();
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root, 614, 461));

        Splash controller=loader.getController();
        controller.stage=stage;
        controller.stage.initOwner(parentStage);
        controller.stage.titleProperty().bindBidirectional(controller.titleLabel.textProperty());
        return controller;
    }

    public StringProperty titleProperty() {
        return titleLabel.textProperty();
    }

    public StringProperty subtitleProperty() {
        return subtitleLabel.textProperty();
    }

    public DoubleProperty progressProperty() {
        return progressIndicator.progressProperty();
    }

    public StringProperty messageProperty(){
        return processLabel.textProperty();
    }

    public void hide() {
        stage.hide();
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
        stage.getScene().getWindow().setY(stage.getScene().getWindow().getY() + 113);//TODO WHY???
    }

    public void runTask() {
        if (task == null) {
            throw new NullPointerException("Task is null! Use getAwaitSplash(CancellableTask<V>,Stage) to get proper splash!");
        }
        stage.show();
        stage.centerOnScreen();
        stage.getScene().getWindow().setY(stage.getScene().getWindow().getY() + 113);//TODO WHY???
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            new Thread(task).start();
        }).start();
    }
}
