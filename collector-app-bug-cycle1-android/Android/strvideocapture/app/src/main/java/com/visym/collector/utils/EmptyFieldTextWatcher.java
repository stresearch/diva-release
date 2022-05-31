package com.visym.collector.utils;

import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputLayout;

public class EmptyFieldTextWatcher implements TextWatcher {
  private TextInputLayout mTextInputLayout;

  public EmptyFieldTextWatcher(TextInputLayout inputLayout){
    this.mTextInputLayout = inputLayout;
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
      if(s.length() > 0){
          mTextInputLayout.setError(null);
          mTextInputLayout.setErrorEnabled(false);
      }else {
        mTextInputLayout.setError("Required field");
        mTextInputLayout.setErrorEnabled(true);
      }
  }

  @Override
  public void afterTextChanged(Editable s) {

  }
}
