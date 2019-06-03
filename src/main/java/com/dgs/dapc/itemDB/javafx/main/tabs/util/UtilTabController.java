package com.dgs.dapc.itemDB.javafx.main.tabs.util;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.headless.db.DiscriminatedObjectId;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.HashMap;
import java.util.Map;

public class UtilTabController {
    public MainController mainController;

    public static final char CLEAR_QUERY='q',CLEAR_TAG='t',CLEAR_LOCATION='l',CLEAR_DESIGNATION='d',CLEAR_CONTACT='c';

    public static final Map<Character,ModeQR> MODE_QR_MAP=new HashMap<>();

    public enum ModeQR{
        ADD('+',"Mode Add 1"),SUB('-',"Mode Sub 1"),
        SUB_ALL('_',"Mode Sub All"),REMOVE('<',"Mode Remove"),
        EDIT('=',"Mode Edit"),QUERY('?',"Mode Query Only");

        public final char discriminator;
        public final String name;

        ModeQR(char discriminator,String name){
            this.discriminator=discriminator;
            this.name=name;
            MODE_QR_MAP.put(discriminator,this);
        }

    }

    public void clearQueryPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(CLEAR_QUERY));
    }

    public void clearTagPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(CLEAR_TAG));
    }

    public void clearLocationPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(CLEAR_LOCATION));
    }

    public void clearDesignationPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(CLEAR_DESIGNATION));
    }

    public void clearContactPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(CLEAR_CONTACT));
    }

    public void addPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.ADD.discriminator));
    }

    public void subPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.SUB.discriminator));
    }

    public void subAllPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.SUB_ALL.discriminator));
    }

    public void editPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.EDIT.discriminator));
    }

    public void removePrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.REMOVE.discriminator));
    }

    public void queryPrint(ActionEvent actionEvent) {
        PrintQR.print(new DiscriminatedObjectId(ModeQR.QUERY.discriminator));
    }

    public void reloadAll(ActionEvent actionEvent) {
        if(mainController.editors.isEmpty()) {
            performReload();
        }else {
            Alert alert=new Alert(Alert.AlertType.WARNING,"Close all editors and reload?",ButtonType.OK,ButtonType.CANCEL);
            alert.initOwner(mainController.getStage());
            if(ButtonType.OK==alert.showAndWait().orElse(ButtonType.CANCEL)){
                killEditors(actionEvent);
                if(mainController.editors.isEmpty()){
                    performReload();
                }else {
                    Alert failed = new Alert(Alert.AlertType.ERROR, "Reload Failed!");
                    failed.initOwner(mainController.getStage());
                    failed.showAndWait();
                }
            }
        }
    }

    public void killEditors(ActionEvent actionEvent) {
        for (Object o : mainController.editors.toArray()) {
            if (o instanceof IWindowInitialize) {
                ((IWindowInitialize) o).getStage().getOnCloseRequest().handle(null);
                ((IWindowInitialize) o).getStage().close();
            }
        }
    }

    private void performReload(){
        mainController.unloadRecords();
        mainController.model.logic.reload();
    }
}
