package com.service.rtochallan;

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.text.TextUtils;
import android.widget.EditText;

import java.text.ParseException;
import java.util.Locale;

public class FormValidator {

    public static boolean validateRequired(EditText editText, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validatePassword(EditText editText, String errorMessage){

        String text = editText.getText().toString().trim();
        String passwordPattern = "^(?=.*\\d)(?=.*[@#$%^&+=]).+$";
        if (!text.matches(passwordPattern)) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return false;
        }
        return true;
    }


    public static boolean validateEmail(EditText editText, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text) || !android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            editText.setError(errorMessage);
            return false;
        }
        return true;
    }
    public static boolean validatePhoneNumber(EditText editText, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text) || !android.util.Patterns.PHONE.matcher(text).matches()) {
            editText.setError(errorMessage);
            return false;
        }
        return true;
    }

    public static boolean validateMinLength(EditText editText, int minLength, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (text.length() < minLength) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean validateMaxLength(EditText editText, int maxLength, String errorMessage) {
        String text = editText.getText().toString().trim();
        if (text.length() > maxLength) {
            editText.setError(errorMessage);
            return false;
        }
        return true;
    }

    public static boolean validatePANCard(EditText editText, String errorMessage) {
        String editTextString = editText.getText().toString().trim();
        String panCardPattern = "[A-Z]{5}[0-9]{4}[A-Z]{1}";
        if(editTextString.matches(panCardPattern)) {
            return true;
        } else {
            editText.setError("Invalid pan card number");
            return false;
        }
    }
    public static boolean validateDate(EditText editText, String errorMessage) {
        String date = editText.getText().toString().trim();
        String datePattern = "dd/MM/yyyy";
        SimpleDateFormat sdf = new
                SimpleDateFormat(datePattern, Locale.US);
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            editText.setError(null);
            return true;
        } catch (ParseException e) {
            editText.setError(errorMessage);
            return false;
        }
    }

    public static boolean validateExpiryDate(EditText editText, String errorMessage) {
        String date = editText.getText().toString().trim();
        String datePattern = "MM/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern, Locale.US);
        sdf.setLenient(false);

        try {
            sdf.parse(date);
            Calendar expiryDate = Calendar.getInstance();
            expiryDate.setTime(sdf.parse(date));
            expiryDate.set(Calendar.DAY_OF_MONTH, expiryDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            if (expiryDate.before(Calendar.getInstance())) {
                editText.setError(errorMessage);
                return false;
            }
            editText.setError(null);
            return true;
        } catch (ParseException e) {
            editText.setError(errorMessage);
            return false;
        }
    }

}
