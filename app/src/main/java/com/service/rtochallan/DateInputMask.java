package com.service.rtochallan;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class DateInputMask implements TextWatcher {

    private final EditText editText;
    private String current = "";

    public DateInputMask(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        String input = s.toString();
        if (!input.equals(current)) {
            String cleanInput = input.replaceAll("[^\\d]", "");

            // Ensure day and month have leading zero if necessary
            if (cleanInput.length() >= 2 && Character.getNumericValue(cleanInput.charAt(0)) > 3) {
                cleanInput = "0" + cleanInput;
            } else if (cleanInput.length() > 4 && Character.getNumericValue(cleanInput.charAt(2)) > 1) {
                cleanInput = cleanInput.substring(0, 2) + "0" + cleanInput.substring(2);
            }

            StringBuilder formatted = new StringBuilder();
            int index = 0;

            for (char ch : cleanInput.toCharArray()) {
                if (index == 2 || index == 4) {
                    formatted.append("/");
                }
                formatted.append(ch);
                index++;
            }
            current = formatted.toString();
            editText.removeTextChangedListener(this);
            editText.setText(current);
            editText.setSelection(current.length());
            editText.addTextChangedListener(this);
        }
    }
}
