package com.dgs.dapc.itemDB.headless.properties;

import com.dgs.dapc.itemDB.Utility;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class ObservableToStringList<V extends Property> extends ObservableListWrapper<V> {
    public final ReadOnlyStringWrapper toString =new ReadOnlyStringWrapper();
    {
        toString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                ObservableToStringList.this.addListener((ListChangeListener<V>) change -> {
                    while (change.next()) {
                        if (change.wasRemoved()) {
                            change.getRemoved().forEach(this::unbind);
                            invalidate();
                        }
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(this::bind);
                            invalidate();
                        }
                    }
                });
            }

            @SuppressWarnings("unchecked")
            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                ObservableToStringList.this.forEach(v -> {
                    String s=stringConverter.toString(v.getValue());
                    if(s!=null && s.length()>0) {
                        stringBuilder.append(s).append(Utility.LIST_SEPARATOR);
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - Utility.LIST_SEPARATOR.length());
                    return stringBuilder.toString();
                }
                return "";
            }
        });
    }
    private StringConverter stringConverter=new StringConverter() {
        @Override
        public String toString(Object object) {
            return object.toString();
        }

        @Override
        public Object fromString(String string) {
            return null;
        }
    };

    public ObservableToStringList(StringConverter stringConverter){
        this();
        this.stringConverter=stringConverter;
    }

    public ObservableToStringList(List<V> list, StringConverter stringConverter){
        this(list);
        this.stringConverter=stringConverter;
    }

    public ObservableToStringList(List<V> list) {
        super(list);
    }

    public ObservableToStringList(){
        this(new ArrayList<>());
    }

    @Override
    public String toString() {
        return toString.get();
    }

    public ReadOnlyStringProperty toStringProperty() {
        return toString.getReadOnlyProperty();
    }
}
