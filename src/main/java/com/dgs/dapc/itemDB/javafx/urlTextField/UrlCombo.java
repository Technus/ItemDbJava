package com.dgs.dapc.itemDB.javafx.urlTextField;

import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.converter.DefaultStringConverter;

import java.net.URISyntaxException;
import java.util.function.Consumer;

public class UrlCombo extends ComboBox<String> {
    public static Consumer<String> URL_LAUNCHER =s->{};

    {
        try {
            getStylesheets().add(UrlCombo.class.getResource("arrow.css").toURI().toString());
        }catch (URISyntaxException e){
            e.printStackTrace();
        }
        setEditable(true);
        setConverter(new DefaultStringConverter());

        itemsProperty().bind(new ObjectBinding<ObservableList<String>>() {
            @Override
            protected ObservableList<String> computeValue() {
                return null;
            }
        });

        setOnShowing(event -> {
            URL_LAUNCHER.accept(getValue());
            hide();
            event.consume();
        });
        setOnShown(event -> {
            hide();
            event.consume();
        });
    }

}
