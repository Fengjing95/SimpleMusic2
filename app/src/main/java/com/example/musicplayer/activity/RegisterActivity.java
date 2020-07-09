package com.example.musicplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.R;
import com.example.musicplayer.useLitepal.UserLitePal;


import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    //手机号编辑框

    private TextView reg_mobile;
    //密码编辑框

    private TextView reg_password;
    //注册按钮

    private Button btn_regSuc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        reg_mobile = (TextView) findViewById(R.id.reg_mobile);
        reg_password = (TextView) findViewById(R.id.reg_password);
        btn_regSuc = (Button) findViewById(R.id.btn_regSuc);
        btn_regSuc.setOnClickListener(this);
        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_regSuc:
                //得到输入框的内容
                String phonenum = reg_mobile.getText().toString();
                String password = reg_password.getText().toString();
                //判断输入是否合法
                if (phonenum.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!Pattern.compile("^[A-Za-z0-9]+$").matcher(phonenum).matches() ||
                        phonenum.length() < 6 || phonenum.length() > 12) {
                    Toast.makeText(this, "用户名或密码只能为6至12位英文或数字", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!Pattern.compile("1[345789][0-9]{9}").matcher(phonenum).matches()) {
                    Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //注册
                    UserLitePal user = new UserLitePal(phonenum, password);
                    user.save();
                    //成功
                    Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent();
//                    intent.setClass(this, LoginActivity.class);
//                    startActivity(intent);
                    finish();
                }
                break;
            default:

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
