package com.example.nasa;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class PhotoActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {   //将PhotoFragment托管到PhotoActivity上
        return PhotoFragment.newInstance();
    }
}
