package Domain;

public class Administrator extends User {

    public Administrator(String username) {
        super(username);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
