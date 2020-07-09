package com.example.musicplayer;


import org.litepal.crud.LitePalSupport;

public class UserDTO extends LitePalSupport {



    private String phoneNum;

    private String password;


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public UserDTO(String phoneNum, String password) {
        this.phoneNum = phoneNum;
        this.password = password;
    }
}
