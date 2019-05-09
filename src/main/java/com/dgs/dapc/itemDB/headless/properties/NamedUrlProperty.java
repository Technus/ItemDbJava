package com.dgs.dapc.itemDB.headless.properties;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;

public class NamedUrlProperty extends SimpleStringProperty {
    public NamedUrlProperty() {}

    public NamedUrlProperty(String initialValue) {
        super(initialValue);
    }

    private final ReadOnlyStringWrapper name=new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper link=new ReadOnlyStringWrapper();
    {
        name.bind(new StringBinding() {
            {
                bind(NamedUrlProperty.this);
            }
            @Override
            protected String computeValue() {
                String namedUrl=NamedUrlProperty.this.get();
                if (namedUrl == null) {
                    return null;
                }
                String[] pieces=namedUrl.split("#");
                switch (pieces.length){
                    case 0:return null;
                    case 1:return pieces[0];
                    case 2:return pieces[0].length()==0?pieces[1]:pieces[0];
                    default: return namedUrl;
                }
            }
        });
        link.bind(new StringBinding() {
            {
                bind(NamedUrlProperty.this);
            }
            @Override
            protected String computeValue() {
                String namedUrl=NamedUrlProperty.this.get();
                if (namedUrl == null) {
                    return null;
                }
                String[] pieces=namedUrl.split("#");
                switch (pieces.length){
                    case 0:return null;
                    case 1:return pieces[0];
                    case 2:return pieces[1];
                    default: return namedUrl;
                }
            }
        });
    }

    @Override
    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    public String getLink() {
        return link.get();
    }

    public ReadOnlyStringProperty linkProperty() {
        return link.getReadOnlyProperty();
    }
}
