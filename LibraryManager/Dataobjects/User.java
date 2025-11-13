public class User {

    protected String username;

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    /* Is normaly set to false can only be overridden by admin subclass */
    public boolean isAdmin() {
        return false;
    }
}
