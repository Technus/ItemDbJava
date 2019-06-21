package com.dgs.dapc.itemDB.javafx.nullComboBox;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class NullComboTableCell<S,T> extends TextFieldTableCell<S,T> {
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forColumn(
            BiConsumer<NullCombo<T>,StringConverter<T>> onCreated,
            StringConverter<T> converter) {
        return list -> {
            NullComboTableCell<S,T> cell=new NullComboTableCell<>(converter);
            cell.onTextFieldCreated=onCreated;
            return cell;
        };
    }

    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forColumn(
            BiConsumer<NullCombo<T>,StringConverter<T>> onCreated) {
        return list -> {
            NullComboTableCell<S,T> cell=new NullComboTableCell<>();
            cell.onTextFieldCreated=onCreated;
            return cell;
        };
    }

    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forColumn() {
        return list -> new NullComboTableCell<>();
    }

    public NullComboTableCell() {
    }

    public NullComboTableCell(StringConverter<T> converter) {
        super(converter);
    }

    {
        editingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && getTextField() == null) {
                setTextField(createTextField());
            }
        });
    }

    private Field textField;
    {
        try {
            textField = TextFieldTreeTableCell.class.getDeclaredField("textField");
            textField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setTextField(TextField field){
        try {
            textField.set(this,field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private TextField getTextField(){
        try {
            return (TextField)textField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private EventHandler<ActionEvent> onAction;
    private EventHandler<? super KeyEvent> onKeyReleased;

    private TextField createTextField() {
        final NullCombo<T> textField = new NullCombo<>();
        //textField.setText(getItemText(cell, converter));
        onTextFieldCreated.accept(textField, getConverter());
        textField.setNullableValue(getItem());

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        onAction = textField.getOnAction();
        textField.setOnAction(event -> {
            if (onAction != null) onAction.handle(event);
            commitEdit(textField.getNullableValue());
            event.consume();
        });
        onKeyReleased = textField.getOnKeyReleased();
        textField.setOnKeyReleased(t -> {
            if (onKeyReleased != null) onKeyReleased.handle(t);
            if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                t.consume();
            }
        });
        return textField;
    }

    private BiConsumer<NullCombo<T>,StringConverter<T>> onTextFieldCreated=(nullCombo,converter) -> {};
}
