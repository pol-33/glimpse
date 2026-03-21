package model;

public class User {
    private String username;
    private String name;
    private String surname;
    private String email;
    private String password;

    public User(String username, String name, String surname, String email, String password) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
    }

    // Getters
    public String getUsername() { return username; }
    public String getName()     { return name; }
    public String getSurname()  { return surname; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setName(String name)         { this.name = name; }
    public void setSurname(String surname)   { this.surname = surname; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}