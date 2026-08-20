package com.gestpov.desktop.ui.products;

import com.gestpov.desktop.ui.Reloadable;
import com.gestpov.desktop.session.SessionContext;
import javafx.scene.layout.StackPane;

public final class ProductWorkspace extends StackPane implements Reloadable {

    private final SessionContext session;
    private final ProductsView listView;

    public ProductWorkspace(SessionContext session) {
        this.session = session;
        this.listView = new ProductsView(session, this::openCreate, this::openEdit);
        getChildren().setAll(listView);
    }

    private void openCreate() {
        getChildren().setAll(new ProductFormView(session, null, this::backToList, this::openEdit));
    }

    private void openEdit(long id) {
        getChildren().setAll(new ProductFormView(session, id, this::backToList, this::openEdit));
    }

    @Override
    public void reload() {
        getChildren().setAll(listView);
        listView.reload();
    }

    private void backToList() {
        getChildren().setAll(listView);
        listView.reload();
    }
}
