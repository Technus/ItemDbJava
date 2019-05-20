package com.dgs.dapc.itemDB.javafx;

import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.MainModel;
import com.dgs.dapc.itemDB.javafx.splash.Splash;
import com.dgs.dapc.itemDB.javafx.urlTextField.UrlCombo;
import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.util.List;
import java.util.Locale;

public class App extends Application {
    /* todo:
    - druk
    - zestawy
    - location tab!
    - logging?
     */

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader=new FXMLLoader(MainController.class.getResource("MainView.fxml"));
        Parent parent= loader.load();
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(e->Platform.exit());
        primaryStage.setScene(new Scene(parent));
        primaryStage.titleProperty().setValue("ItemDB");

        Splash splash=Splash.getInstance(primaryStage);
        splash.titleProperty().setValue("ItemDB");
        List<String> parameters=getParameters().getRaw();
        splash.subtitleProperty().setValue("Starting Application");

        Task<Void> task=new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (SplashScreen.getSplashScreen() != null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                    SplashScreen.getSplashScreen().close();
                }
                int workAmount=5,workDone=0;

                Thread.setDefaultUncaughtExceptionHandler((t,e)->{
                    e.printStackTrace();
                    Platform.exit();
                });
                Thread.currentThread().setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
                updateMessage("Loading main controller");
                updateProgress(workDone,workAmount);
                MainController controller=loader.getController();

                controller.hostServices=getHostServices();
                UrlCombo.URL_LAUNCHER= s -> {
                    String[] pieces=s.split("#");
                    switch (pieces.length){
                        case 1:s=pieces[0];break;
                        case 2:s=pieces[1];break;
                        default: return;
                    }
                    if(s==null || s.length()==0){
                        return;
                    }
                    controller.hostServices.showDocument(s);
                };

                updateMessage("Loading main model");
                updateProgress(++workDone,workAmount);
                controller.model=new MainModel(parameters);

                updateProgress(++workDone,workAmount);
                Thread.setDefaultUncaughtExceptionHandler((t,e)->{
                    controller.model.showThrowableMain(e);
                    Platform.exit();
                });
                Thread.currentThread().setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());

                updateProgress(++workDone,workAmount);
                updateMessage("Loading database");
                controller.model.logic.reload();

                updateProgress(++workDone,workAmount);


                updateMessage("Loading data");
                controller.itemsController.reloadAll();
                controller.sourcesController.reloadAll();
                updateProgress(++workDone,workAmount);

                updateMessage("Loading finished");
                Thread.sleep( 500);

                Platform.runLater(()-> {
                    primaryStage.show();
                    splash.hide();
                });
                return null;
            }
        };
        splash.progressProperty().bind(task.progressProperty());
        splash.messageProperty().bind(task.messageProperty());

        splash.show();
        new Thread(task).start();
    }

    public static void main(String... args) {
        Locale.setDefault(Locale.US);
        try {
            setUserAgentStylesheet(STYLESHEET_MODENA);
            Thread.sleep(10);
            StyleManager.getInstance().addUserAgentStylesheet(App.class.getResource("modena_dark.css").toString());
            Thread.sleep(10);
            launch(App.class,args);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }
}
