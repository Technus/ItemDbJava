package com.dgs.dapc.itemDB.javafx.splash;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;

public abstract class SplashTask<V> extends Task<V> {
    private final ReadOnlyBooleanWrapper isNotCancellable =new ReadOnlyBooleanWrapper(true);

    public SplashTask(String title, String message, V defaultValue){
        updateTitle(title);
        updateMessage(message);
        updateValue(defaultValue);
    }

    @Override
    protected abstract V call() throws Exception;

    public ReadOnlyBooleanProperty isNotCancellableProperty(){
        return isNotCancellable.getReadOnlyProperty();
    }

    public boolean getNotCancellable(){
        return isNotCancellable.get();
    }

    protected void setNotCancellable(boolean cancellable){
        isNotCancellable.set(cancellable);
    }

    public void updateProgress(){
        super.updateProgress(-1,-1);
    }
}
