package com.dgs.dapc.itemDB.javafx.main;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.MainLogic;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class MainModel {
    public final MainLogic logic;

    public Region defaultRegion;//default place for error log
    private final Stage stage;

    public MainModel(List<String> parameters, Stage stage){
        logic=new MainLogic(parameters);
        this.stage=stage;
    }

    //region throwable gui
    public ButtonType showConfirmThrowableMain(Throwable throwable, ButtonType... buttonTypes){
        return showConfirmThrowable(defaultRegion,throwable,buttonTypes);
    }

    public void showThrowableMain(Throwable throwable){
        showThrowable(defaultRegion,throwable);
    }

    public ButtonType showConfirmThrowableMain(Region component, Throwable throwable, ButtonType... buttonTypes){
        return showConfirmThrowable(component==null?defaultRegion:component,throwable,buttonTypes);
    }

    public void showThrowableMain(Region component,Throwable throwable){
        showThrowable(component==null?defaultRegion:component,throwable);
    }

    private void showThrowable(Region component,Throwable throwable){
        showConfirmThrowable(component,throwable);
    }

    private ButtonType showConfirmThrowable(Region region,Throwable throwable,ButtonType... buttonTypes){
        try{
            logic.logError(throwable);
        }catch (Exception e){
            throwable.printStackTrace();
        }

        Alert.AlertType type=throwable instanceof Exception? Alert.AlertType.WARNING: Alert.AlertType.ERROR;
        Alert alert = new Alert(type,null,buttonTypes);
        alert.setTitle(throwable.getClass().getSimpleName());
        alert.setHeaderText(throwable.getLocalizedMessage());
        ScrollPane scrollPane=new ScrollPane();
        Label label=new Label(Utility.throwableToString(throwable));
        scrollPane.setContent(label);
        scrollPane.setPrefHeight(400);
        scrollPane.setPrefWidth(600);
        alert.getDialogPane().setContent(scrollPane);
        alert.setResizable(true);
        if(region!=null) {
            Bounds bounds = region.localToScreen(region.getBoundsInLocal());
            if(bounds!=null) {
                alert.setX(bounds.getMinX() + (region.getWidth() - alert.getWidth()) / 2D);
                alert.setY(bounds.getMinY() + (region.getHeight() - alert.getHeight()) / 2D);
            }
        }
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(stage);
        ((Stage) alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
        try {
            Optional<ButtonType> buttonTypeOptional = alert.showAndWait();
            return buttonTypeOptional.orElse(ButtonType.CLOSE);
        }catch (IllegalStateException e){
            alert.setOnHidden(event -> Platform.exit());
            alert.show();
            return ButtonType.CLOSE;
        }
    }
    //endregion
}
