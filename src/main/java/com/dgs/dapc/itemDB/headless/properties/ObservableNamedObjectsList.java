package com.dgs.dapc.itemDB.headless.properties;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.INamed;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.List;

public class ObservableNamedObjectsList<V extends INamed> extends ObservableListWrapper<V> {
    public final ReadOnlyStringWrapper toString =new ReadOnlyStringWrapper();
    {
        toString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                ObservableNamedObjectsList.this.addListener((ListChangeListener<V>) change -> {
                    while (change.next()) {
                        if (change.wasRemoved()) {
                            change.getRemoved().forEach(named -> unbind(named.nameProperty()));
                            invalidate();
                        }
                        if (change.wasAdded()) {
                            change.getAddedSubList().forEach(named -> bind(named.nameProperty()));
                            invalidate();
                        }
                    }
                });
            }

            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                ObservableNamedObjectsList.this.forEach(v -> {
                    String s=v.getName();
                    if(s!=null && s.length()>0){
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

    public ObservableNamedObjectsList(List<V> list) {
        super(list);
    }

    public ObservableNamedObjectsList(){
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
