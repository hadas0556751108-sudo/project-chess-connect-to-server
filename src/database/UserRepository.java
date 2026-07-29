package database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import model.User;
import org.bson.Document;

public class UserRepository {
    private final MongoClient client;
    private final MongoCollection<Document> users;

    public UserRepository(String mongoUri) {
        this.client = MongoClients.create(mongoUri);
        MongoDatabase db = client.getDatabase("chess");
        this.users = db.getCollection("users");
    }

    public void init() {
        users.createIndex(new Document("username", 1), new IndexOptions().unique(true));
    }

    public boolean createUser(User user) {
        try {
            Document doc = new Document("username", user.getUsername())
                    .append("password_hash", user.getPasswordHash())
                    .append("elo", user.getElo());
            users.insertOne(doc);
            return true;
        } catch (com.mongodb.MongoWriteException e) {
            if (e.getError().getCategory() == com.mongodb.ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    public User findByUsername(String username) {
        Document doc = users.find(new Document("username", username)).first();
        if (doc == null) {
            return null;
        }
        return new User(
                doc.getString("username"),
                doc.getString("password_hash"),
                doc.getInteger("elo", 1200)
        );
    }

    public boolean updateElo(String username, int elo) {
        Document result = users.findOneAndUpdate(
                new Document("username", username),
                new Document("$set", new Document("elo", elo)),
                new FindOneAndUpdateOptions().upsert(false)
        );
        return result != null;
    }

    public boolean exists(String username) {
        return findByUsername(username) != null;
    }

    public void close() {
        client.close();
    }
}
