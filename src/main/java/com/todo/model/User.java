package com.todo.model;

//import java.util.concurrent.atomic.AtomicInteger;

public class User {
	
	
	private final int userid;
    private String username;
    private String password;
//	private static final AtomicInteger idCounter = new AtomicInteger(0);
    
	public User(int userid, String username, String password) {
	    this.userid = userid;
	    this.username = username;
	    this.password = password;
	}
        
    
    public int getUserid() {
		return userid;
	}

    public void setUsername(String username) {
		this.username = username;
	}

    public String getUsername() {
       
		return username;
    }

    public String getPassword() {
        return password;
}

    public boolean isValid() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }

	public boolean login(String username, String password) {
		return username != null && password != null &&
				username.equals(this.username) && password.equals(this.password);
	}
}