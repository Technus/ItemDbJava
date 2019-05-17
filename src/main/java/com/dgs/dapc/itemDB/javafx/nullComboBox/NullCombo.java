package com.dgs.dapc.itemDB.javafx.nullComboBox;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class NullCombo<T> extends TextField {
    private final ContextMenu contextMenu=new ContextMenu();
    private final ObservableList<MenuItem> backingItems= FXCollections.observableArrayList();
    {
        backingItems.addListener((ListChangeListener<MenuItem>) c -> {
            contextMenu.getItems().setAll(backingItems);
            contextMenu.hide();
        });
    }

    private final SimpleObjectProperty<BiFunction<String,T,Boolean>> filter=new SimpleObjectProperty<>((s, t) -> true);
    {
        contextMenu.setMaxHeight(300);
        contextMenu.setPrefHeight(200);
        contextMenu.setMinHeight(100);
        contextMenu.addEventHandler(Menu.ON_SHOWING, e -> {
            Node content = contextMenu.getSkin().getNode();
            if (content instanceof Region) {
                ((Region) content).setMaxHeight(contextMenu.getMaxHeight());
                ((Region) content).setMinHeight(contextMenu.getMinHeight());
            }
        });
        contextMenu.setOpacity(0.95D);

        setStyle("-fx-control-inner-background: derive(-fx-base,+7%);");

        setOnMouseClicked(event -> {
            if(!contextMenu.isShowing()){
                contextMenu.show(NullCombo.this, Side.RIGHT, 0, 0);
            }
        });
        setOnKeyTyped(event -> {
            //if(event.getCharacter().charAt(0)=='\r'|| event.getCharacter().charAt(0)=='\n'){
            //    commitEdit();
            //    event.consume();
            //}
        });
        setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                commitEdit();
                return;
            }else if(event.getCode()==KeyCode.TAB || event.getCode()==KeyCode.ESCAPE){
                return;
            }
            if(!contextMenu.isShowing()) {
                contextMenu.show(NullCombo.this, Side.RIGHT, 0, 0);
            }
        });
        textProperty().addListener((observable, oldValue, newValue) -> {
            backingItems.forEach(this::setVisible);
        });
        filter.addListener((observable, oldValue, newValue) -> {
            if(newValue==null){
                setFilter((s,t)->true);
            }else{
                backingItems.forEach(this::setVisible);
            }
        });
    }
    private final SimpleBooleanProperty anyFocused=new SimpleBooleanProperty();
    private final SimpleStringProperty nullString =new SimpleStringProperty();
    private final MenuItem nullItem=new MenuItem();
    {
        nullItem.setStyle("-fx-text-fill:-fx-prompt-text-fill;");
    }

    private final SimpleObjectProperty<T> nullableValue =new SimpleObjectProperty<T>(){
        @Override
        public void set(T newValue) {
            super.set(newValue);
            setText(newValue==null?null:newValue.toString());
        }
    };
    private final ReadOnlyBooleanWrapper nullSelected =new ReadOnlyBooleanWrapper(true);
    {
        contextMenu.setOnHiding(event -> {
            if(isFocused()){
                Platform.runLater(()->{
                    if(isFocused()) contextMenu.show(NullCombo.this,Side.RIGHT,0,0);
                    else contextMenu.hide();
                });
            }
        });
        contextMenu.setOnShowing(event -> {
            backingItems.remove(nullItem);
            backingItems.forEach(menuItem -> {
                menuItem.setVisible(true);
            });
            backingItems.sort(Comparator.comparing(menuItem -> menuItem.getUserData().toString()));
            backingItems.add(0,nullItem);
        });
        contextMenu.setOnAction(event -> {
            if(event.getTarget() instanceof MenuItem){
                if(event.getTarget()==nullItem){
                    setNullableValue(null);
                }else {
                    setNullableValue((T)((MenuItem) event.getTarget()).getUserData());
                }
            }
        });
        anyFocused.bind(new BooleanBinding() {
            {
                bind();
                bind(contextMenu.focusedProperty());
            }
            @Override
            protected boolean computeValue() {
                return isFocused()||contextMenu.isFocused();
            }
        });
        anyFocused.addListener((observable, oldValue, newValue) -> {
            if(newValue){
                contextMenu.show(NullCombo.this, Side.RIGHT,0,0);
                selectAll();
            }else{
                if(contextMenu.isShowing()) {
                    commitEdit();
                } else if(getNullableValue()!=null) {
                    setText(getNullableValue().toString());
                }
            }
        });
        setOnAction(event -> {
            commitEdit();
        });
        nullString.addListener((observable, oldValue, newValue) -> {
            nullItem.setText(newValue);
        });
        nullString.set("Deselect");
        setPromptText("Select");
    }

    private final ListChangeListener<T> changeListener= c -> {
        ArrayList<MenuItem> toRemove=new ArrayList<>();
        while(c.next()){
            if(c.wasRemoved()){
                backingItems.forEach(menuItem -> {
                    if(menuItem!=nullItem && c.getRemoved().contains(menuItem.getUserData())){
                        toRemove.add(menuItem);
                    }
                });
                toRemove.forEach(menuItem -> backingItems.remove(menuItem));
                toRemove.clear();
            }
            if(c.wasAdded()){
                if(c.getAddedSubList().size()>0){
                    c.getAddedSubList().forEach(t->{
                        MenuItem item=new MenuItem(t.toString());
                        item.setUserData(t);
                        setVisible(item);
                        backingItems.add(item);
                    });
                }
            }
        }
        if(!c.getList().contains(getNullableValue())){
            setNullableValue(null);
        }
    };
    private final SimpleObjectProperty<ObservableList<T>> backingList=new SimpleObjectProperty<>();
    {
        backingList.addListener((observable, oldValue, newValue) -> {
            if(oldValue!=null){
                oldValue.removeListener(changeListener);
            }
            backingItems.clear();
            if(newValue!=null){
                newValue.addListener(changeListener);
                newValue.forEach(t->{
                    MenuItem item=new MenuItem(t.toString());
                    item.setUserData(t);
                    setVisible(item);
                    backingItems.add(item);
                });
                backingItems.add(0,nullItem);
                if(!newValue.contains(getNullableValue())){
                    setNullableValue(null);
                }
            }else{
                setNullableValue(null);
            }
        });
        nullSelected.bind(new BooleanBinding() {
            {
                bind(nullableValue);
            }
            @Override
            protected boolean computeValue() {
                return getNullableValue() == null;
            }
        });
    }

    private void commitEdit(){
        if(getText()==null || getText().length()==0){
            setNullableValue(null);
            return;
        }
        Object newVal=backingItems.stream()
                .filter(MenuItem::isVisible)
                .filter(menuItem -> menuItem!=nullItem)
                .findFirst().orElse(nullItem).getUserData();
        if(newVal==null){
            setNullableValue(null);
        }else {
            setNullableValue((T)newVal);
        }
        selectAll();
        contextMenu.hide();
    }

    private ArrayList<MenuItem> temp=new ArrayList<>();

    private void setVisible(MenuItem item){
        if(item!=nullItem){
            if(textProperty().getValueSafe().length()==0){
                item.setVisible(true);
            }else {
                item.setVisible(filter.get().apply(textProperty().getValueSafe(),item.getUserData()==null?null:(T)item.getUserData()));
            }
        }
    }

    public ObservableList getBackingList() {
        return backingList.get();
    }

    public SimpleObjectProperty<ObservableList<T>> backingListProperty() {
        return backingList;
    }

    public void setBackingList(ObservableList<T> backingList) {
        this.backingList.set(backingList);
    }

    public String getNullString() {
        return nullString.get();
    }

    public SimpleStringProperty nullStringProperty() {
        return nullString;
    }

    public void setNullString(String nullString) {
        this.nullString.set(nullString);
    }

    public boolean isNullSelected() {
        return nullSelected.get();
    }

    public ReadOnlyBooleanProperty nullSelectedProperty() {
        return nullSelected.getReadOnlyProperty();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(backingList.get()!=null){
            backingList.get().removeListener(changeListener);
        }
    }

    public void setRegexPredicate(){
        filter.set(new BiFunction<String, T, Boolean>() {
            private Pattern pattern = Pattern.compile("");

            @Override
            public Boolean apply(String s, T t) {
                try {
                    String regexp;
                    if(!s.startsWith("(?")){
                        regexp="(?i)" +s;
                    }else{
                        regexp=s;
                    }
                    pattern = Pattern.compile(regexp);
                } catch (Exception ignored) {
                    pattern = Pattern.compile(Pattern.quote(s));
                }

                return t!=null && pattern.matcher(t.toString()).find();
            }
        });
    }
    {
        setRegexPredicate();
    }
    public void setPredicate(){
        filter.set((s,t)->{
            if(t==null){
                return false;
            }
            String vString=s.toLowerCase();
            String tString=t.toString().toLowerCase();
            return tString.startsWith(vString) || tString.endsWith(vString);
        });
    }

    public T getNullableValue() {
        return nullableValue.get();
    }

    public SimpleObjectProperty<T> nullableValueProperty() {
        return nullableValue;
    }

    public void setNullableValue(T nullableValue) {
        this.nullableValue.set(nullableValue);
    }

    public BiFunction<String, T, Boolean> getFilter() {
        return filter.get();
    }

    public SimpleObjectProperty<BiFunction<String, T, Boolean>> filterProperty() {
        return filter;
    }

    public void setFilter(BiFunction<String, T, Boolean> filter) {
        this.filter.set(filter);
    }
}
