package com.dgs.dapc.itemDB.javafx.nullComboBox;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class NullCombo<T> extends ComboBox<T> {
    private final SimpleObjectProperty<ObservableList<T>> backingList=new SimpleObjectProperty<>(FXCollections.emptyObservableList());
    private final SimpleStringProperty nullText=new SimpleStringProperty(null);
    private final SimpleObjectProperty<T> nullObject=new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper isNullSelected=new ReadOnlyBooleanWrapper(true);
    private final SimpleObjectProperty<Predicate<T>> filter=new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<T> nullableValue=new ReadOnlyObjectWrapper<>();
    {
        setPredicate();

        nullObject.addListener((observable, oldValue, newValue) -> setItems());
        backingList.addListener((observable, oldValue, newValue) -> setItems());

        setCellFactory(param -> new ListCell<T>(){
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item != null ? item.toString() : getNullText());
                    setStyle(item == getNullObject() || item == null ? "-fx-text-fill:-fx-prompt-text-fill;" : "");
                }
            }
        });

        setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object==null?null:object.toString();
            }

            @Override
            public T fromString(String string) {
                if(string==null || string.length()==0){
                    return getNullObject();
                }
                FilteredList<T> list=getBackingList().filtered(getFilter());
                if(list.contains(getValue())){
                    return getValue();//restores previous value if it still matches
                }
                return list.size()>0?list.get(0):getNullObject();
            }
        });

        isNullSelected.bind(new BooleanBinding() {
            {
                bind(itemsProperty());
                bind(valueProperty());
            }
            @Override
            protected boolean computeValue() {
                return valueProperty().get()==null || valueProperty().get()==getNullObject();
            }
        });

        nullableValue.bind(new ObjectBinding<T>() {
            {
                bind(itemsProperty());
                bind(valueProperty());
            }
            @Override
            protected T computeValue() {
                return valueProperty().get()==getNullObject()?null:valueProperty().get();
            }
        });

        setOnKeyReleased(new AutoCompleteComboBoxListener());

        valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue==getNullObject()){
                Platform.runLater(()->setValue(null));
            }
        });
    }

    private void setItems(){
        ObservableList<T> list = FXCollections.observableArrayList();
        list.add(getNullObject());
        list.addAll(getBackingList());
        setItems(list);
        setValue(getNullObject());
    }

    public ObservableList<T> getBackingList() {
        return backingList.get();
    }

    public SimpleObjectProperty<ObservableList<T>> backingListProperty() {
        return backingList;
    }

    public void setBackingList(ObservableList<T> backingList) {
        this.backingList.set(backingList);
    }

    public T getNullObject() {
        return nullObject.get();
    }

    public SimpleObjectProperty<T> nullObjectProperty() {
        return nullObject;
    }

    public void setNullObject(T nullObject) {
        this.nullObject.set(nullObject);
    }

    public class AutoCompleteComboBoxListener implements EventHandler<KeyEvent> {
        private boolean moveCaretToPos = false;
        private int caretPos;

        private AutoCompleteComboBoxListener() {
            setEditable(true);
            //setOnKeyPressed(t -> hide());
            setOnShowing(event -> process());
        }

        @Override
        public void handle(KeyEvent event) {
            if(event.getCode() == KeyCode.UP) {
                if(isShowing()) {
                    hide();
                }
                caretPos = -1;
                moveCaret(getEditor().getText().length());
                return;
            } else if(event.getCode() == KeyCode.DOWN) {
                if(!isShowing()) {
                    show();
                }
                caretPos = -1;
                moveCaret(getEditor().getText().length());
                return;
            } else if(event.getCode() == KeyCode.BACK_SPACE) {
                moveCaretToPos = true;
                caretPos = getEditor().getCaretPosition();
            } else if(event.getCode() == KeyCode.DELETE) {
                moveCaretToPos = true;
                caretPos = getEditor().getCaretPosition();
            }

            if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT
                    || event.isControlDown() || event.getCode() == KeyCode.HOME
                    || event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB) {
                return;
            }

            moveCaretToPos = true;
            caretPos = getEditor().getCaretPosition();

            if(!getItems().isEmpty()) {
                show();
            }
        }

        private void process(){
            int count=getItems().size();
            String temp=getEditor().getText();

            ObservableList<T> list = FXCollections.observableArrayList();
            list.add(getNullObject());
            FilteredList<T> filtered=getBackingList().filtered(getFilter());
            list.addAll(filtered.size()==0?getBackingList():filtered);
            setItems(list);

            if(isShowing() && count<getItems().size() && count<getVisibleRowCount()){
                hide();
                setOnShowing(e -> {});
                show();
                setOnShowing(e -> process());
            }
            Platform.runLater(()->{
                if(!moveCaretToPos) {
                    caretPos = -1;
                }
                getEditor().setText(temp);
                moveCaret(temp.length());
            });
        }

        private void moveCaret(int textLength) {
            if(caretPos == -1) {
                getEditor().positionCaret(textLength);
            } else {
                getEditor().positionCaret(caretPos);
            }
            moveCaretToPos = false;
        }
    }

    public boolean isNullSelected() {
        return isNullSelected.get();
    }

    public ReadOnlyBooleanProperty isNullSelectedProperty() {
        return isNullSelected.getReadOnlyProperty();
    }

    public Predicate<T> getFilter() {
        return filter.get();
    }

    public SimpleObjectProperty<Predicate<T>> filterProperty() {
        return filter;
    }

    public void setFilter(Predicate<T> filter) {
        this.filter.set(filter);
    }

    public T getNullableValue() {
        return nullableValue.get();
    }

    public ReadOnlyObjectProperty<T> nullableValueProperty() {
        return nullableValue.getReadOnlyProperty();
    }

    public String getNullText() {
        return nullText.get();
    }

    public SimpleStringProperty nullTextProperty() {
        return nullText;
    }

    public void setNullText(String nullText) {
        this.nullText.set(nullText);
    }

    @Override
    @Deprecated
    public ObjectProperty<T> valueProperty() {
        return super.valueProperty();
    }

    public void setRegexPredicate(){
        setFilter(new Predicate<T>() {
            private Pattern pattern = Pattern.compile("");

            @Override
            public boolean test(T t) {
                String valueSafe = NullCombo.this.getEditor().textProperty().getValueSafe();

                valueSafe="(?i)" +valueSafe;

                try {
                    pattern = Pattern.compile(valueSafe);
                } catch (Exception ignored) {
                    pattern = Pattern.compile(Pattern.quote(valueSafe));
                }

                return t!=null && pattern.matcher(t.toString()).find();
            }
        });
    }
    public void setPredicate(){
        setFilter(t->{
            if(t==null){
                return false;
            }
            String vString=getEditor().textProperty().getValueSafe().toLowerCase();
            String tString=t.toString().toLowerCase();
            return tString.startsWith(vString) || tString.endsWith(vString);
        });
    }
}
