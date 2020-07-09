package com.example.musicplayer.useLitepal;


import org.litepal.crud.LitePalSupport;

public class UserLitePal extends LitePalSupport {



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

    public UserLitePal(String phoneNum, String password) {
        this.phoneNum = phoneNum;
        this.password = password;
    }
}
