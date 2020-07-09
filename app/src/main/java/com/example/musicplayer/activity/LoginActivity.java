package com.example.musicplayer.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.R;

import org.litepal.LitePal;

import java.util.concurrent.TimeUnit;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "LoginActivity";
    //手机号编辑框

    private TextView et_mobile;
    //密码编辑框

    private TextView et_password;
    //登录按钮
    private Button btn_login;
    //注册按钮
    private Button btn_register;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        et_mobile =  findViewById(R.id.et_mobile);
        et_password =  findViewById(R.id.et_password);
        btn_login =  findViewById(R.id.btn_login);
        btn_register =  findViewById(R.id.btn_register);
        btn_login.setOnClickListener(this);
        btn_register.setOnClickListener(this);
        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);
        Toast.makeText(LoginActivity.this, "hello world", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
//                Toast.makeText(LoginActivity.this, "hello", Toast.LENGTH_SHORT).show();
                //得到输入框的内容
                String phonenum = et_mobile.getText().toString();
                String password = et_password.getText().toString();

                if(phonenum.isEmpty() || password.isEmpty()){
                    Toast.makeText(this,"电话号码或密码为空，请重新输入！",Toast.LENGTH_SHORT).show();
                    break;
                }

                //匹配数据库电话号码与密码
                Boolean flag = findByPhoneNum(phonenum,password);
                Log.d(TAG, "onClick: flag"+flag.toString());
                Toast.makeText(this,"flag"+flag.toString(),Toast.LENGTH_SHORT).show();
                if(!flag){
                    Toast.makeText(this,"电话号码或密码错误，请重新输入！",Toast.LENGTH_SHORT).show();
                    break;
                }else{
//                    Toast.makeText(this,"登录成功",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
//                    Intent intent = new Intent(this,MainActivity.class);
//                    startActivity(intent);
                    intent.putExtra("username",phonenum);
                    setResult(2,intent);
                    finish();
                    break;
                }

            case R.id.btn_register:
                Toast.makeText(LoginActivity.this, "hello 2", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this,RegisterActivity.class);
                startActivity(intent);
                break;
            default:

        }
    }


    //根据电话号码和密码查找用户
    public Boolean findByPhoneNum (String phoneNum, String password){
        Cursor cursor = LitePal.findBySQL("select * from UserLitePal where phoneNum = ? and password = ?", phoneNum, password);
        return cursor != null;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

}
