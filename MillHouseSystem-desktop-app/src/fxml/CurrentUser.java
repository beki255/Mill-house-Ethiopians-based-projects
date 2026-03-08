package fxml;

public class CurrentUser {
    // The single instance
    private static CurrentUser instance;

    // User data fields
    private int staffId;
    private String staffName;
    private String username;
    private String role;

    // Private constructor to prevent direct instantiation
    private CurrentUser() {}

    // Thread-safe lazy initialization (safe for most desktop apps)
    public static CurrentUser getInstance() {
        if (instance == null) {
            instance = new CurrentUser();
        }
        return instance;
    }

    // Optional: more performant double-checked locking (if you care about multi-threading)
    // public static CurrentUser getInstance() {
    //     if (instance == null) {
    //         synchronized (CurrentUser.class) {
    //             if (instance == null) {
    //                 instance = new CurrentUser();
    //             }
    //         }
    //     }
    //     return instance;
    // }

    public void setStaffInfo(int id, String name, String username, String role) {
        this.staffId = id;
        this.staffName = name;
        this.username = username;
        this.role = role;
    }

    public int getStaffId() {
        return staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void clear() {
        staffId = 0;
        staffName = null;
        username = null;
        role = null;
        // Optionally: instance = null; if you want to fully reset singleton
    }
}