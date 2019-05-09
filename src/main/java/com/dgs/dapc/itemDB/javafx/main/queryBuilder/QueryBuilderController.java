package com.dgs.dapc.itemDB.javafx.main.queryBuilder;

import com.mongodb.QueryBuilder;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.net.URL;
import java.util.ResourceBundle;

public class QueryBuilderController implements Initializable {
    public TreeTableView<IQueryPart> queryTree;
    public TreeTableColumn<IQueryPart, Boolean> logicColumn;
    public TreeTableColumn<IQueryPart, ComboBox<String>> fieldColumn;
    public TreeTableColumn<IQueryPart, ComboBox<String>> operationColumn;
    public TreeTableColumn<IQueryPart, IQueryPart> valueColumn;

    private final SimpleObjectProperty<QueryGroup> query = new SimpleObjectProperty<>(new QueryGroup());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        queryTree.rootProperty().bind(new ObjectBinding<TreeItem<IQueryPart>>() {
            {
                bind(query);
            }

            @Override
            protected TreeItem<IQueryPart> computeValue() {
                return query.get()!=null?query.get().getTreeItem():null;
            }
        });

        logicColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof QueryGroup){
                return ((QueryGroup) param.getValue().getValue()).orLogicProperty();
            }
            return null;
        });
    }

    public ComboBox<String> getFieldNameBox(ObservableList<String> names){
        ComboBox<String> box=new ComboBox<>(names);
        box.setEditable(true);
        return box;
    }

    public QueryBuilder build(){
        return query.get().getBuilder();
    }
}
