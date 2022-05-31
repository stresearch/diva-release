package com.visym.collector.utils;

import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputLayout;

public class EmailFieldTextWatcher implements TextWatcher {

  TextInputLayout mTextInputLayout;

  public EmailFieldTextWatcher(TextInputLayout mTextInputLayout){
    this.mTextInputLayout = mTextInputLayout;
  }
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    if(s.length() > 0){
      if(Globals.isValidEmail(mTextInputLayout.getEditText().getText().toString().trim())){
        mTextInputLayout.setError(null);
        mTextInputLayout.setErrorEnabled(false);
      }else{
        mTextInputLayout.setError("Invalid email address");
        mTextInputLayout.setErrorEnabled(true);
      }
    }
  }

  @Override
  public void afterTextChanged(Editable s) {

  }
}
