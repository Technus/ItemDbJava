package com.dgs.dapc.itemDB.javafx.nullComboBox;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MultiCombo<T> extends TextField {
    private volatile boolean isShowingValues;
    private final ContextMenu contextMenu=new ContextMenu();
    private final ObservableList<MenuItem> backingItems= FXCollections.observableArrayList();
    private final SimpleObjectProperty<BiFunction<String,T,Boolean>> filter=new SimpleObjectProperty<>((s, t) -> true);
    private final SimpleStringProperty nullString =new SimpleStringProperty();
    private final MenuItem nullItem=new MenuItem();
    private final ReadOnlyBooleanWrapper nullSelected =new ReadOnlyBooleanWrapper(true);
    private final SimpleObjectProperty<ObservableList<T>> backingList=new SimpleObjectProperty<>();
    private final ObservableList<T> nullableValue =FXCollections.observableArrayList();
    private final ListChangeListener<T> backingListChangeListener = c -> {
        ArrayList<MenuItem> work=new ArrayList<>();
        while(c.next()){
            if(c.wasRemoved() && c.getRemoved().size()>0){
                backingItems.forEach(menuItem -> {
                    if(menuItem!=nullItem && c.getRemoved().contains(menuItem.getUserData())){
                        work.add(menuItem);
                    }
                });
                if(work.size()>0) {
                    backingItems.removeAll(work);
                    work.clear();
                }
            }
            if(c.wasAdded() && c.getAddedSubList().size()>0) {
                c.getAddedSubList().forEach(t -> {
                    MenuItem item = new MenuItem(t.toString());
                    item.setUserData(t);
                    setVisible(item);
                    work.add(item);
                });
                backingItems.addAll(work);
                work.clear();
            }
        }
        nullableValue.removeAll(nullableValue.parallelStream().filter(t -> !c.getList().contains(t)).collect(Collectors.toList()));
    };
    {
        nullableValue.addListener((ListChangeListener<T>) c -> {
            StringBuilder stringBuilder=new StringBuilder();
            c.getList().forEach(t ->{
                stringBuilder.append(t.toString()).append(", ");
            });
            if(stringBuilder.length()>0){
                stringBuilder.setLength(stringBuilder.length()-2);
            }
            setText(stringBuilder.toString());
            isShowingValues=true;
            nullSelected.set(nullableValue.size()==0);
        });

        setPromptText("Select");
        setStyle("-fx-control-inner-background: derive(-fx-base,+8%);");
        nullString.set("Deselect");
        nullItem.setStyle("-fx-text-fill:-fx-prompt-text-fill;");

        contextMenu.setMaxHeight(300);
        contextMenu.setPrefHeight(200);
        contextMenu.setMinHeight(200);
        contextMenu.addEventHandler(Menu.ON_SHOWING, e -> {
            Node content = contextMenu.getSkin().getNode();
            if (content instanceof Region) {
                ((Region) content).setMaxHeight(contextMenu.getMaxHeight());
                ((Region) content).setMinHeight(contextMenu.getMinHeight());
            }
        });
        contextMenu.setOpacity(0.95D);
        contextMenu.setOnAction(event -> {
            if(event.getTarget() instanceof MenuItem){
                if(event.getTarget()==nullItem){
                    nullableValue.clear();
                }else {
                    nullableValue.setAll((T)((MenuItem) event.getTarget()).getUserData());
                }
                Platform.runLater(this::showWithAll);
            }
        });

        textProperty().addListener((observable, oldValue, newValue) -> {
            backingItems.forEach(this::setVisible);
            backingItems.setAll(new ArrayList<>(backingItems));
            if(isFocused()) {
                try {
                    contextMenu.show(MultiCombo.this, Side.RIGHT, 0, 0);
                } catch (Exception ignored) {
                }
            }
            isShowingValues =false;
        });
        nullableValue.clear();

        setOnKeyTyped(event -> {
            if(event.getCharacter().charAt(0)=='\r'|| event.getCharacter().charAt(0)=='\n'){
                if(isShowingValues){
                    return;
                }else if (textProperty().getValueSafe().length()==0) {
                    nullableValue.clear();
                }else{
                    commitEdit();
                }
                contextMenu.hide();
            }
        });
        setOnMouseClicked(event -> {
            if(!contextMenu.isShowing()) {
                if(textProperty().getValueSafe().length()==0 || isShowingValues){
                    showWithAll();
                }else {
                    contextMenu.show(MultiCombo.this,Side.RIGHT,0,0);
                }
                selectAll();
            }
        });
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                if(!contextMenu.isShowing() && !isShowingValues) {
                    if(textProperty().getValueSafe().length()==0 ){
                        showWithAll();
                    }else {
                        contextMenu.show(MultiCombo.this,Side.RIGHT,0,0);
                    }
                }
                selectAll();
            }else{
                if(!isShowingValues) {
                    commitEdit();
                }
                //contextMenu.hide();
            }
        });

        backingItems.addListener((ListChangeListener<MenuItem>) c -> {
            contextMenu.getItems().setAll(backingItems.filtered(MenuItem::isVisible));
        });
        backingList.addListener((observable, oldValue, newValue) -> {
            if(oldValue!=null){
                oldValue.removeListener(backingListChangeListener);
            }
            if(newValue!=null){
                ArrayList<MenuItem> items=new ArrayList<>();
                items.add(nullItem);
                newValue.forEach(t->{
                    MenuItem item=new MenuItem(t.toString());
                    item.setUserData(t);
                    setVisible(item);
                    items.add(item);
                });
                backingItems.setAll(items);
                newValue.addListener(backingListChangeListener);
                nullableValue.removeAll(nullableValue.parallelStream().filter(t -> !newValue.contains(t)).collect(Collectors.toList()));
            }else{
                backingItems.setAll(nullItem);
                nullableValue.clear();
            }
        });

        filter.addListener((observable, oldValue, newValue) -> {
            if(newValue==null){
                setFilter((s,t)->true);
            }else{
                backingItems.stream().skip(1).forEach(this::setVisible);
                backingItems.setAll(new ArrayList<>(backingItems));
            }
        });
        nullString.addListener((observable, oldValue, newValue) -> {
            nullItem.setText(newValue);
        });
        //nullSelected.bind(new BooleanBinding() {
        //    {
        //        bind(nullableValue);
        //    }
        //    @Override
        //    protected boolean computeValue() {
        //        return nullableValue.size()==0;
        //    }
        //});
    }

    private void showWithAll(){
        backingItems.stream().skip(1).forEach(menuItem -> {
            if(!menuItem.isVisible()){
                menuItem.setVisible(true);
            }
            menuItem.setText(menuItem.getUserData().toString());
        });
        backingItems.sort((o1, o2) -> {
            if(o2==nullItem){
                return 1;
            }else if(o1==nullItem){
                return -1;
            }
            return o1.textProperty().getValueSafe().compareTo(o2.textProperty().getValueSafe());
        });
        backingItems.setAll(new ArrayList<>(backingItems));
        contextMenu.show(MultiCombo.this,Side.RIGHT,0,0);
    }

    private void setVisible(MenuItem item){
        if(item!=nullItem){
            if(textProperty().getValueSafe().length()==0){
                item.setVisible(true);
            }else {
                item.setVisible(filter.get().apply(textProperty().getValueSafe(),item.getUserData()==null?null:(T)item.getUserData()));
            }
        }
    }

    private void commitEdit(){
        if(getText()==null || getText().length()==0){
            nullableValue.clear();
            return;
        }
        nullableValue.setAll(contextMenu.getItems().stream().skip(1)
                .map(menuItem -> (T)menuItem.getUserData()).collect(Collectors.toList()));
        selectAll();
    }

    private ArrayList<MenuItem> temp=new ArrayList<>();

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
        if(contextMenu.isShowing()){
            contextMenu.hide();
        }
        if(backingList.get()!=null){
            backingList.get().removeListener(backingListChangeListener);
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

    public ObservableList<T> nullableValueProperty() {
        return nullableValue;
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
