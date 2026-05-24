package packagee.model;

public interface IUser {

    long getId();

    String getUsername();

    String getFirstname();

    String getLastname();

    String getPassword();

    void setUsername(String username);

    void setFirstname(String firstname);

    void setLastname(String lastname);

    void setPassword(String password);
}
