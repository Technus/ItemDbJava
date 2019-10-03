package com.dgs.dapc.itemDB.javafx.main.tabs.contacts;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.ILinked;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Contact;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.contactEditor.ContactEditorController;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.converter.DefaultStringConverter;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.Document;

import java.net.URL;
import java.util.ResourceBundle;

public class ContactsTabController implements Initializable {
    public TableView<Contact> contactsTable;
    public TableColumn<Contact,String> contactsNameColumn;
    public TableColumn<Contact, String> contactsUrlColumn;
    public TableColumn<Contact,String> contactsDetailsColumn;
    public MainController mainController;
    public Button newBasedOnButton;
    public Button removeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        contactsTable.setItems(Contact.COLLECTION.readableAndSortableList);
        contactsTable.setRowFactory( tv -> {
            TableRow<Contact> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY)
                    e.consume();
            });
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY && !row.isEmpty()) {
                    Contact rowData = row.getItem();
                    Utility.Window<ContactEditorController> window=Utility.loadFXML(ContactEditorController.class.getResource("ContactEditor.fxml"),"Contact Editor: "+rowData.getId().toHexString(),mainController.getStage());
                    window.controller.setMainController(mainController);
                    window.controller.setContact(rowData);
                    window.stage.show();
                    e.consume();
                }
            });
            return row;
        });

        contactsNameColumn.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            mainController.model.logic.getContactCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())),event.getRowValue());
            //contactsTable.refresh();//todo needed?
        });
        contactsNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        contactsNameColumn.setCellValueFactory(param-> param.getValue().nameProperty());

        contactsUrlColumn.setOnEditCommit(event -> {
            event.getRowValue().setUrl(event.getNewValue());
            contactsTable.refresh();
            mainController.model.logic.getContactCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())),event.getRowValue());
            //contactsTable.refresh();//todo needed?
        });
        contactsUrlColumn.setCellValueFactory(param-> param.getValue().urlProperty().nameProperty());
        contactsUrlColumn.setCellFactory(param -> {
            TextFieldTableCell<Contact,String> cell=new TextFieldTableCell<>();
            cell.setOnMouseClicked(event -> {
                if(cell.getTableRow().getItem() instanceof ILinked){
                    Contact link=(Contact)cell.getTableRow().getItem();
                    if(link.urlProperty().linkProperty().getValueSafe().length()>0){
                        mainController.hostServices.showDocument(link.urlProperty().getLink());
                    }
                }
            });
            cell.setConverter(new DefaultStringConverter());
            cell.setStyle("-fx-text-fill:-fx-text-blue;");
            return cell;
        });

        contactsDetailsColumn.setOnEditCommit(event -> {
            event.getRowValue().setDetails(event.getNewValue());
            mainController.model.logic.getContactCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())),event.getRowValue());
            //contactsTable.refresh();//todo needed?
        });
        contactsDetailsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        contactsDetailsColumn.setCellValueFactory(param-> param.getValue().detailsProperty());

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(contactsTable.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                return contactsTable.getSelectionModel().getSelectedItem()==null;
            }
        });

        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(contactsTable.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                return contactsTable.getSelectionModel().getSelectedItem() == null;
            }
        });
    }

    public void create(ActionEvent actionEvent) {
        Contact contact=new Contact();
        Utility.Window<ContactEditorController> window=Utility.loadFXML(ContactEditorController.class.getResource("ContactEditor.fxml"),"Contact Editor: "+contact.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setContact(contact);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Contact contact=contactsTable.getSelectionModel().getSelectedItem().cloneObjectData();
        Utility.Window<ContactEditorController> window=Utility.loadFXML(ContactEditorController.class.getResource("ContactEditor.fxml"),"Contact Editor: "+contact.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setContact(contact);
        window.stage.show();
    }

    public void removeSelected(ActionEvent actionEvent) {
        Contact contact=contactsTable.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Remove "+contact+" ?");
        alert.initOwner(mainController.getStage());
        if(ButtonType.OK==alert.showAndWait().orElse(ButtonType.CANCEL)){
            mainController.model.logic.getContactCollection().deleteOne(new Document("_id",contact.getId()));
            Contact.COLLECTION.map.remove(contact.getId());
        }
    }
}
