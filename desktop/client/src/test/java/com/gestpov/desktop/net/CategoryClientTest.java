package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Category;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryClientTest {

    @Test
    void treeContainsParentAndChild() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            List<Category> tree = client.getTree();
            assertEquals(1, tree.size());
            assertEquals("Électronique", tree.get(0).nom());
            assertEquals(1, tree.get(0).childrenOrEmpty().size());
            assertEquals("Téléphones", tree.get(0).childrenOrEmpty().get(0).nom());
            assertEquals(tree.get(0).id(), tree.get(0).childrenOrEmpty().get(0).parentId());
        }
    }

    @Test
    void createRootAndChild_thenRefresh() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category sport = client.create("Sport", null);
            assertNull(sport.parentId());

            Category shoes = client.create("Chaussures", sport.id());
            assertEquals(sport.id(), shoes.parentId());

            List<Category> tree = client.getTree();
            assertEquals(2, tree.size());
            Category sportNode = tree.stream().filter(c -> "Sport".equals(c.nom())).findFirst().orElseThrow();
            assertEquals(1, sportNode.childrenOrEmpty().size());
            assertEquals("Chaussures", sportNode.childrenOrEmpty().get(0).nom());
        }
    }

    @Test
    void searchReturnsFlatListWithParentName() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            List<Category> found = client.search("tél");
            assertEquals(1, found.size());
            assertEquals("Téléphones", found.get(0).nom());
            assertEquals("Électronique", found.get(0).parentNom());
        }
    }

    @Test
    void renameKeepsParent() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category child = client.getTree().get(0).childrenOrEmpty().get(0);
            Category updated = client.update(child.id(), "Smartphones", child.parentId());
            assertEquals("Smartphones", updated.nom());
            assertEquals(child.parentId(), updated.parentId());
        }
    }

    @Test
    void reattachToRoot_andToAnotherParent() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category sport = client.create("Sport", null);
            Category child = client.getTree().get(0).childrenOrEmpty().get(0);

            Category moved = client.update(child.id(), child.nom(), sport.id());
            assertEquals(sport.id(), moved.parentId());

            Category root = client.update(child.id(), child.nom(), null);
            assertNull(root.parentId());
        }
    }

    @Test
    void selfParentRejected() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category root = client.getTree().get(0);
            ApiException ex = assertThrows(ApiException.class,
                    () -> client.update(root.id(), root.nom(), root.id()));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().contains("propre parent"));
        }
    }

    @Test
    void deleteWithChildrenRejected_thenDeleteChildThenParent() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category parent = client.getTree().get(0);
            Category child = parent.childrenOrEmpty().get(0);

            ApiException ex = assertThrows(ApiException.class, () -> client.delete(parent.id()));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().contains("sous-catégories"));

            client.delete(child.id());
            client.delete(parent.id());
            assertTrue(client.getTree().isEmpty());
        }
    }

    @Test
    void deleteLinkedToProductsRejected() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            Category linked = client.create("Linked", null);
            ApiException ex = assertThrows(ApiException.class, () -> client.delete(linked.id()));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().contains("produits sont rattachés"));
        }
    }

    @Test
    void blankNameRejected() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CategoryClient client = client(server);
            ApiException ex = assertThrows(ApiException.class, () -> client.create("", null));
            assertEquals(400, ex.statusCode());
            assertEquals("Le nom est obligatoire", ApiException.userMessage(ex));
        }
    }

    @Test
    void categoriesWithoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            CategoryClient client = new CategoryClient(api);
            ApiException ex = assertThrows(ApiException.class, client::getTree);
            assertEquals(401, ex.statusCode());
        }
    }

    private static CategoryClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new CategoryClient(api);
    }
}
